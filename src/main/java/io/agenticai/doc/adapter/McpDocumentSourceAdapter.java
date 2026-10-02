package io.agenticai.doc.adapter;

import io.agenticai.doc.domain.FetchMode;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.DocumentSourceQuery;
import io.agenticai.doc.port.DocumentSourceQueryPort;
import io.agenticai.doc.port.DocumentSourceRow;
import io.agenticai.platform.mcp.McpQueryChannel;
import io.agenticai.reg.contract.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link DocumentSourceQueryPort} for the {@code path} fetch mode (REQ-DOC-004, ADR-DOC-001):
 * runs the document source query through the platform MCP query channel. The channel is taken as
 * an {@link ObjectProvider} because no delivered package defines its bean yet; without one every
 * {@code path} query is a recorded SOURCE_QUERY_FAILED and nothing is called.
 *
 * <p>Every run goes through the same steps, in this order:
 * <ol>
 *   <li>RULE-DOC-007 — no connection settings (REG answered "not found") → SOURCE_QUERY_FAILED
 *       with the rule's message, nothing sent (REQ-DOC-052);</li>
 *   <li>no channel bean → SOURCE_QUERY_FAILED, nothing sent;</li>
 *   <li>the Check's deadline already reached → SOURCE_QUERY_FAILED, nothing sent (REQ-DOC-040);</li>
 *   <li>one call: the SQL text exactly as stored (REQ-DOC-051), the request number as the single
 *       bound value of {@code :{inputName}} (REQ-DOC-004; G4), the type and path columns as the
 *       only columns asked for — document content never travels through MCP (REQ-DOC-016;
 *       G14) — the limit {@code maxRows + 1} and the deadline;</li>
 *   <li>more than {@code maxRows} rows → the whole result SOURCE_QUERY_FAILED, no row kept
 *       (REQ-DOC-039);</li>
 *   <li>each row → its type and path; a row without one of the two columns → SOURCE_QUERY_FAILED
 *       naming the column, never its data.</li>
 * </ol>
 *
 * <p>Failure translation (A.4.9, E.1.5): any exception out of the channel becomes a recorded
 * SOURCE_QUERY_FAILED outcome naming the exception's type and message, logged at debug with the
 * cause. Neither the SQL text, nor the request number, nor any row reaches a log or a detail.
 * Stateless: nothing is kept between calls (G9).
 */
@Component
public class McpDocumentSourceAdapter implements DocumentSourceQueryPort {

    private static final Logger log = LoggerFactory.getLogger(McpDocumentSourceAdapter.class);

    private final ObjectProvider<McpQueryChannel> channel;

    public McpDocumentSourceAdapter(ObjectProvider<McpQueryChannel> channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    @Override
    public FetchMode mode() {
        return FetchMode.PATH;
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
        if (query.locationColumn() == null) {
            return unreadable("the document source of \"" + query.serviceCode()
                    + "\" names no path column; the path query was not run");
        }
        // 2. the platform channel must be present in this deployable
        McpQueryChannel queryChannel = channel.getIfAvailable();
        if (queryChannel == null) {
            return unreadable("the platform MCP query channel is not available in this deployable");
        }
        // 3. none of the Check's time left → nothing sent (REQ-DOC-040)
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            return unreadable("the Check's timeout was reached before the document source query was run");
        }
        // 4. one call: the stored text, the bound value, the two columns, maxRows + 1 (REQ-DOC-016, G14)
        List<String> columns = List.of(query.documentTypeColumn(), query.locationColumn());
        log.debug("DOC document source (path): running the query of \"{}\" over connection \"{}\" "
                        + "through the MCP channel, limit {} rows, {} ms left",
                query.serviceCode(), connection.connectionName(), query.rowLimit(), remaining.toMillis());
        List<Map<String, Object>> rows;
        try {
            rows = queryChannel.execute(connection, query.sqlText(), query.inputName(), query.requestNumber(),
                    columns, query.rowLimit(), deadline);
        } catch (Exception e) {
            // translation only: the channel's failure becomes the query's recorded outcome
            log.debug("DOC document source (path): the query of \"{}\" over connection \"{}\" failed: {}",
                    query.serviceCode(), connection.connectionName(), DocumentSourceGuards.describe(e), e);
            return unreadable("the document source query failed: " + DocumentSourceGuards.describe(e));
        }
        if (rows == null) {
            return unreadable("the document source query returned no result through the MCP channel");
        }
        // 5. over the limit → the whole result fails, no row kept (REQ-DOC-039)
        if (rows.size() > query.maxRows()) {
            return unreadable("the document source query returned more than " + query.maxRows()
                    + " rows (aias.check.max-rows); no document was fetched");
        }
        // 6. the type and the path of each row — nothing else is read
        List<DocumentSourceRow> result = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            if (row == null) {
                return unreadable("row " + (index + 1) + " of the document source query is absent");
            }
            if (!hasColumn(row, query.documentTypeColumn())) {
                return unreadable(missingColumn(query.documentTypeColumn(), index));
            }
            if (!hasColumn(row, query.locationColumn())) {
                return unreadable(missingColumn(query.locationColumn(), index));
            }
            result.add(DocumentSourceRow.pathRow(
                    text(columnValue(row, query.documentTypeColumn())),
                    text(columnValue(row, query.locationColumn()))));
        }
        log.debug("DOC document source (path): {} row(s) returned for \"{}\"", result.size(), query.serviceCode());
        return new Read<>(List.copyOf(result));
    }

    private static String missingColumn(String column, int index) {
        return "row " + (index + 1) + " of the document source query has no column \"" + column + "\"";
    }

    /** Whether the row carries the column, by its name as the host returned it, case-insensitively. */
    private static boolean hasColumn(Map<String, Object> row, String column) {
        if (row.containsKey(column)) {
            return true;
        }
        for (String key : row.keySet()) {
            if (key != null && key.equalsIgnoreCase(column)) {
                return true;
            }
        }
        return false;
    }

    private static Object columnValue(Map<String, Object> row, String column) {
        if (row.containsKey(column)) {
            return row.get(column);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static ReadOutcome<List<DocumentSourceRow>> unreadable(String detail) {
        return new Unreadable<>(UnreadableReason.SOURCE_QUERY_FAILED, detail);
    }
}
