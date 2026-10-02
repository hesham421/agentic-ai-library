package io.agenticai.doc.adapter;

import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.DocumentSourceQuery;
import io.agenticai.reg.contract.ConnectionSettings;

/**
 * The guards both document source adapters run <strong>before</strong> any query, and the one
 * place their messages are spelled (RULE-DOC-006, RULE-DOC-007 — SRS texts, verbatim). Each
 * refusal is a recorded SOURCE_QUERY_FAILED outcome carrying the rule's message as detail
 * (REQ-DOC-014, REQ-DOC-052; ADR-DOC-007), never an exception.
 *
 * <p>RULE-DOC-007 reads "declared read-only", and CON-REG-011 carries no such flag: REG's own
 * schema ({@code CHK_REG_CONNECTION_READ_ONLY}, RULE-REG-015) guarantees that every connection
 * REG supplies is declared read-only, so a connection REG resolved passes. What the guard refuses
 * is the connection REG could <em>not</em> supply — the settings are absent — because nothing
 * then declares it read-only.
 */
final class DocumentSourceGuards {

    /** RULE-DOC-006 message (en); ar PENDING ADR-DOC-012. */
    static final String RULE_DOC_006_MESSAGE =
            "The documents of \"{serviceCode}\" could not be fetched: connection \"{connectionName}\" is not a JDBC connection.";

    /** RULE-DOC-007 message (en); ar PENDING ADR-DOC-012. */
    static final String RULE_DOC_007_MESSAGE =
            "The documents of \"{serviceCode}\" could not be fetched: connection \"{connectionName}\" is not declared read-only.";

    /** The stored value of the {@code jdbc} connection type (CON-REG-005, closed lookup). */
    static final String JDBC_CONNECTION_TYPE = "jdbc";

    private DocumentSourceGuards() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * RULE-DOC-007: the refusal when the query's connection is absent — REG supplied no settings,
     * so the connection is not declared read-only; {@code null} when it is present.
     */
    static <T> Unreadable<T> refuseUnlessDeclaredReadOnly(DocumentSourceQuery query, ConnectionSettings connection) {
        if (connection != null) {
            return null;
        }
        return new Unreadable<>(UnreadableReason.SOURCE_QUERY_FAILED,
                fill(RULE_DOC_007_MESSAGE, query.serviceCode(), query.connectionName()));
    }

    /**
     * RULE-DOC-006: the refusal when a {@code blob} query's connection is not of type
     * {@code jdbc}; {@code null} when it is.
     */
    static <T> Unreadable<T> refuseUnlessJdbc(DocumentSourceQuery query, ConnectionSettings connection) {
        if (JDBC_CONNECTION_TYPE.equals(connection.connectionType())) {
            return null;
        }
        return new Unreadable<>(UnreadableReason.SOURCE_QUERY_FAILED,
                fill(RULE_DOC_006_MESSAGE, query.serviceCode(), connection.connectionName()));
    }

    private static String fill(String template, String serviceCode, String connectionName) {
        return template.replace("{serviceCode}", serviceCode).replace("{connectionName}", connectionName);
    }

    /** The exception's type and message — never the SQL text, a credential or row data. */
    static String describe(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + message;
    }
}
