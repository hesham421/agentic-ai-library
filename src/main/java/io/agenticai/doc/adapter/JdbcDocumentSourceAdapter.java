package io.agenticai.doc.adapter;

import io.agenticai.platform.config.CheckLimitsProperties;
import io.agenticai.doc.domain.BlobContent;
import io.agenticai.doc.domain.FetchMode;
import io.agenticai.doc.domain.FileSize;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.DocumentSourceQuery;
import io.agenticai.doc.port.DocumentSourceQueryPort;
import io.agenticai.doc.port.DocumentSourceRow;
import io.agenticai.reg.contract.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The {@link DocumentSourceQueryPort} for the {@code blob} fetch mode (REQ-DOC-012, REQ-DOC-013,
 * ADR-DOC-001): runs the document source query over the read-only {@code jdbc} connection the
 * query names and reads each row's content column. The only class of DOC that opens a JDBC
 * connection to a host, and it only ever reads: the connection is set read-only before any
 * statement is prepared, the one statement is the stored SELECT, and no write statement exists
 * here or anywhere in DOC (REQ-DOC-049, REQ-DOC-052; G3, G14).
 *
 * <p>Every run goes through the same steps, in this order — the guards before anything is
 * opened (RULE-DOC-006, RULE-DOC-007):
 * <ol>
 *   <li>RULE-DOC-007 — no connection settings (REG answered "not found") → SOURCE_QUERY_FAILED
 *       with the rule's message, nothing opened (REQ-DOC-052);</li>
 *   <li>RULE-DOC-006 — the connection is not of type {@code jdbc} → SOURCE_QUERY_FAILED with the
 *       rule's message, nothing opened (REQ-DOC-014);</li>
 *   <li>the bind parameter {@code :{inputName}} must occur in the stored text — a query that
 *       would have to be changed to carry the request number is refused (REQ-DOC-051);</li>
 *   <li>the credential is resolved from the environment by its reference (ADR-REG-009): until a
 *       secret store is defined, the reference is the name of a Spring {@link Environment}
 *       property — an environment variable qualifies — whose value is {@code username:password};
 *       unresolvable → SOURCE_QUERY_FAILED naming the reference only, no connection attempted.
 *       The credential itself is never logged and never put in a detail;</li>
 *   <li>the Check's deadline already reached → SOURCE_QUERY_FAILED, nothing opened (REQ-DOC-040);</li>
 *   <li>one short-lived connection — {@link DriverManager#getConnection(String, String, String)}
 *       on the connection's endpoint, no pool — set read-only <em>before</em> the statement is
 *       prepared; the statement is the stored text with its single named bind parameter
 *       rewritten to the JDBC placeholder {@code ?} and nothing else changed (REQ-DOC-051); the
 *       request number is bound with {@code setString}, never concatenated (REQ-DOC-012; G4);
 *       {@code setMaxRows(maxRows + 1)} so an over-limit result is seen, not truncated
 *       (REQ-DOC-039); {@code setQueryTimeout} = the Check's remaining time, at least one
 *       second (REQ-DOC-040; G8);</li>
 *   <li>per row: the document type as a String; the content column through {@link Blob#length()}
 *       first and {@link BlobContent#admit} — NOT_FOUND for a NULL or empty column
 *       (REQ-DOC-015), TOO_LARGE for one over {@code aias.check.max-file-size} (REQ-DOC-041,
 *       REQ-DOC-042) — and only on admission {@link Blob#getBinaryStream()} for exactly the
 *       measured length; the row keeps its content's outcome either way and is never dropped
 *       (G6). The Blob is freed after each row;</li>
 *   <li>more than {@code maxRows} rows → the whole result SOURCE_QUERY_FAILED, no row kept
 *       (REQ-DOC-039).</li>
 * </ol>
 *
 * <p>Failure translation (A.4.9, E.1.5): no JDBC exception leaves this class. A
 * {@link SQLTimeoutException} is SOURCE_QUERY_FAILED "timed out"; any other
 * {@link SQLException} is SOURCE_QUERY_FAILED carrying {@code <SQLState>/<Type>: <message>}; a
 * driver's runtime exception, {@code <Type>: <message>}. A detail never carries the SQL text, a
 * credential, the endpoint (a JDBC URL may embed a credential) or row data. The result set, the
 * statement and the connection are closed in {@code finally} order by try-with-resources,
 * whatever happened. Stateless: the limit is read once from platform configuration (CU.3), and
 * nothing is kept between calls (G9).
 */
@Component
public class JdbcDocumentSourceAdapter implements DocumentSourceQueryPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcDocumentSourceAdapter.class);

    /** The JDBC positional placeholder the single named bind parameter is rewritten to. */
    private static final char JDBC_PLACEHOLDER = '?';

    private final Environment environment;
    private final long maxFileSizeBytes;

    public JdbcDocumentSourceAdapter(Environment environment, CheckLimitsProperties limits) {
        this.environment = Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(limits, "limits");
        this.maxFileSizeBytes = limits.maxFileSize().toBytes();
    }

    @Override
    public FetchMode mode() {
        return FetchMode.BLOB;
    }

    @Override
    public ReadOutcome<List<DocumentSourceRow>> run(DocumentSourceQuery query, ConnectionSettings connection,
                                                    Instant deadline) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(deadline, "deadline");

        // 1. RULE-DOC-007: a connection REG could not supply is not declared read-only (REQ-DOC-052)
        Unreadable<List<DocumentSourceRow>> refused = DocumentSourceGuards.refuseUnlessDeclaredReadOnly(query, connection);
        if (refused != null) {
            return refused;
        }
        // 2. RULE-DOC-006: blob documents only over a jdbc connection (REQ-DOC-014)
        refused = DocumentSourceGuards.refuseUnlessJdbc(query, connection);
        if (refused != null) {
            return refused;
        }
        if (query.contentColumn() == null) {
            return unreadable("the document source of \"" + query.serviceCode()
                    + "\" names no content column; the blob query was not run");
        }
        // 3. the stored text must carry the bind parameter; it is rewritten to '?' and nothing else (REQ-DOC-051)
        BoundStatement statement = BoundStatement.of(query.sqlText(), query.inputName());
        if (statement.bindCount() == 0) {
            return unreadable("the document source query does not bind " + query.bindParameter()
                    + "; the request number cannot be passed as a bound value");
        }
        // 4. the credential, by its reference — the secret itself goes nowhere but the driver
        String reference = connection.credentialReference();
        String credential = environment.getProperty(reference);
        if (credential == null || credential.isBlank()) {
            return unreadable("credential reference \"" + reference + "\" is not resolvable in this environment");
        }
        int separator = credential.indexOf(':');
        if (separator < 0) {
            return unreadable("credential reference \"" + reference
                    + "\" does not hold a username:password credential");
        }
        String username = credential.substring(0, separator);
        String password = credential.substring(separator + 1);
        // 5. none of the Check's time left → nothing opened (REQ-DOC-040)
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            return unreadable("the Check's timeout was reached before the document source query was run");
        }
        int timeoutSeconds = querySeconds(remaining);
        log.debug("DOC document source (blob): running the query of \"{}\" over jdbc connection \"{}\", "
                        + "limit {} rows, query timeout {} s", query.serviceCode(), connection.connectionName(),
                query.rowLimit(), timeoutSeconds);

        // 6. one read-only connection, one prepared SELECT, closed in finally order whatever happens
        try (Connection jdbc = DriverManager.getConnection(connection.endpoint(), username, password)) {
            jdbc.setReadOnly(true);
            try (PreparedStatement select = jdbc.prepareStatement(statement.text())) {
                for (int position = 1; position <= statement.bindCount(); position++) {
                    select.setString(position, query.requestNumber());
                }
                select.setMaxRows(query.rowLimit());
                select.setQueryTimeout(timeoutSeconds);
                try (ResultSet rows = select.executeQuery()) {
                    return readRows(rows, query);
                }
            }
        } catch (SQLTimeoutException e) {
            log.debug("DOC document source (blob): the query of \"{}\" over connection \"{}\" timed out after {} s",
                    query.serviceCode(), connection.connectionName(), timeoutSeconds, e);
            return unreadable("the document source query timed out after the Check's remaining "
                    + timeoutSeconds + " s");
        } catch (SQLException e) {
            log.debug("DOC document source (blob): the query of \"{}\" over connection \"{}\" failed: {}",
                    query.serviceCode(), connection.connectionName(), describeSql(e), e);
            return unreadable("the document source query failed: " + describeSql(e));
        } catch (RuntimeException e) {
            // a driver's own runtime failure is translated the same way, never rethrown
            log.debug("DOC document source (blob): the query of \"{}\" over connection \"{}\" failed: {}",
                    query.serviceCode(), connection.connectionName(), DocumentSourceGuards.describe(e), e);
            return unreadable("the document source query failed: " + DocumentSourceGuards.describe(e));
        }
    }

    /** Steps 7 and 8: the rows, each with its content's outcome; the whole result fails over the limit. */
    private ReadOutcome<List<DocumentSourceRow>> readRows(ResultSet rows, DocumentSourceQuery query)
            throws SQLException {
        int typeColumn;
        int contentColumn;
        try {
            typeColumn = rows.findColumn(query.documentTypeColumn());
        } catch (SQLException e) {
            return unreadable("the document source query's result has no column \"" + query.documentTypeColumn() + "\"");
        }
        try {
            contentColumn = rows.findColumn(query.contentColumn());
        } catch (SQLException e) {
            return unreadable("the document source query's result has no column \"" + query.contentColumn() + "\"");
        }
        List<DocumentSourceRow> result = new ArrayList<>();
        while (rows.next()) {
            if (result.size() == query.maxRows()) {
                // the (maxRows + 1)th row exists: over the limit, nothing kept (REQ-DOC-039)
                return unreadable("the document source query returned more than " + query.maxRows()
                        + " rows (aias.check.max-rows); no document was fetched");
            }
            String documentType = rows.getString(typeColumn);
            result.add(DocumentSourceRow.blobRow(documentType, readContent(rows, contentColumn)));
        }
        log.debug("DOC document source (blob): {} row(s) returned for \"{}\"", result.size(), query.serviceCode());
        return new Read<>(List.copyOf(result));
    }

    /**
     * Step 7 for one row: length first, admission, then the stream for exactly that length. A
     * JDBC failure propagates to the query-level translation; a failure of the stream itself is
     * the row's READING_FAILED outcome.
     */
    private ReadOutcome<byte[]> readContent(ResultSet rows, int contentColumn) throws SQLException {
        Blob blob = rows.getBlob(contentColumn);
        try {
            // length first (REQ-DOC-041); a NULL column has none and is NOT_FOUND by the admission
            Long length = blob == null ? null : blob.length();
            ReadOutcome<Long> admitted = BlobContent.admit(length, maxFileSizeBytes);
            if (admitted instanceof Unreadable<Long> notAdmitted) {
                return notAdmitted.retyped();
            }
            // admitted: the column is present, not empty and within the limit
            long size = length;
            if (size > Integer.MAX_VALUE - 8) {
                return new Unreadable<>(UnreadableReason.READING_FAILED,
                        "the content column is " + FileSize.describe(size)
                                + ", more than one array can hold; its content was not read");
            }
            try (InputStream in = blob.getBinaryStream()) {
                byte[] bytes = in.readNBytes((int) size);
                if (bytes.length != size) {
                    return new Unreadable<>(UnreadableReason.READING_FAILED,
                            "the content column streamed " + FileSize.describe(bytes.length) + " of its "
                                    + FileSize.describe(size) + "; its content was not read");
                }
                return new Read<>(bytes);
            } catch (IOException e) {
                log.debug("DOC document source (blob): streaming a content column failed: {}",
                        DocumentSourceGuards.describe(e), e);
                return new Unreadable<>(UnreadableReason.READING_FAILED,
                        "streaming the content column failed: " + DocumentSourceGuards.describe(e));
            }
        } finally {
            if (blob != null) {
                blob.free();
            }
        }
    }

    /** The Check's remaining time in whole seconds, rounded up, at least one (REQ-DOC-040). */
    private static int querySeconds(Duration remaining) {
        long seconds = (remaining.toMillis() + 999L) / 1000L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, seconds));
    }

    /** {@code <SQLState>/<Type>: <message>} — never the SQL text, a credential or row data. */
    private static String describeSql(SQLException e) {
        String state = e.getSQLState() == null || e.getSQLState().isBlank() ? "-" : e.getSQLState();
        return state + "/" + DocumentSourceGuards.describe(e);
    }

    private static ReadOutcome<List<DocumentSourceRow>> unreadable(String detail) {
        return new Unreadable<>(UnreadableReason.SOURCE_QUERY_FAILED, detail);
    }

    /**
     * The stored text with every occurrence of its single named bind parameter rewritten to the
     * JDBC placeholder — the only change ever made to it (REQ-DOC-051). An occurrence is the
     * token {@code :{inputName}} not followed by another identifier character, so
     * {@code :reqno} is never taken for {@code :reqnoX}. Nothing else is touched.
     *
     * @param text      the rewritten text
     * @param bindCount how many placeholders were written — 0 when the text does not bind the
     *                  parameter
     */
    record BoundStatement(String text, int bindCount) {

        static BoundStatement of(String sqlText, String inputName) {
            String token = ":" + inputName;
            StringBuilder rewritten = new StringBuilder(sqlText.length());
            int count = 0;
            int from = 0;
            while (true) {
                int at = sqlText.indexOf(token, from);
                if (at < 0) {
                    rewritten.append(sqlText, from, sqlText.length());
                    break;
                }
                int after = at + token.length();
                if (after < sqlText.length() && isIdentifierCharacter(sqlText.charAt(after))) {
                    // a longer identifier that merely starts with the token: left as it is
                    rewritten.append(sqlText, from, after);
                    from = after;
                    continue;
                }
                rewritten.append(sqlText, from, at).append(JDBC_PLACEHOLDER);
                count++;
                from = after;
            }
            return new BoundStatement(rewritten.toString(), count);
        }

        private static boolean isIdentifierCharacter(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
        }
    }
}
