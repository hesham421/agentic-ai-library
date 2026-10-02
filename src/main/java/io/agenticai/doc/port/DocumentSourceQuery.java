package io.agenticai.doc.port;

import java.util.Objects;

/**
 * One run of a version's document source query (CON-REG-003), as the fetch procedure hands it to
 * the {@link DocumentSourceQueryPort}: the SQL text exactly as the service definition stores it,
 * the name of its single bind parameter, the Check's request number to bind to it, the columns
 * the fetch procedure reads from each row, and the platform's row limit (REQ-DOC-004,
 * REQ-DOC-012, REQ-DOC-039, REQ-DOC-051).
 *
 * <p>The record carries facts only. The SQL text is never changed here, and the request number
 * is never put into it — an adapter binds it under {@link #bindParameter()} (AIAS-5; G4).
 *
 * @param serviceCode        the service whose documents are fetched — fills the
 *                           {@code {serviceCode}} of RULE-DOC-006 / RULE-DOC-007
 * @param connectionName     the connection the query names (CON-REG-003) — fills the
 *                           {@code {connectionName}} of those rules even when REG could not
 *                           supply the connection's settings
 * @param sqlText            the document source query text, exactly as stored (REQ-DOC-051)
 * @param inputName          the version's input name: the query's only parameter is
 *                           {@code :{inputName}} (CON-REG-003)
 * @param requestNumber      the Check's request number, bound to that parameter and never
 *                           concatenated (REQ-DOC-004, REQ-DOC-012)
 * @param documentTypeColumn the column holding each row's document type (REQ-DOC-006)
 * @param locationColumn     the column holding each row's path — {@code path} mode only, else
 *                           {@code null}
 * @param contentColumn      the column holding each row's content — {@code blob} mode only, else
 *                           {@code null} (REQ-DOC-013)
 * @param maxRows            {@code aias.check.max-rows}: more rows than this fail the whole
 *                           query (REQ-DOC-039)
 */
public record DocumentSourceQuery(
        String serviceCode,
        String connectionName,
        String sqlText,
        String inputName,
        String requestNumber,
        String documentTypeColumn,
        String locationColumn,
        String contentColumn,
        int maxRows) {

    public DocumentSourceQuery {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(sqlText, "sqlText");
        Objects.requireNonNull(inputName, "inputName");
        Objects.requireNonNull(requestNumber, "requestNumber");
        Objects.requireNonNull(documentTypeColumn, "documentTypeColumn");
        if (maxRows < 1 || maxRows == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maxRows must be positive and leave room for one more row: " + maxRows);
        }
    }

    /** A {@code path} query: the type and path columns are read, no content (REQ-DOC-016). */
    public static DocumentSourceQuery forPath(String serviceCode, String connectionName, String sqlText,
                                              String inputName, String requestNumber,
                                              String documentTypeColumn, String locationColumn, int maxRows) {
        return new DocumentSourceQuery(serviceCode, connectionName, sqlText, inputName, requestNumber,
                documentTypeColumn, Objects.requireNonNull(locationColumn, "locationColumn"), null, maxRows);
    }

    /** A {@code blob} query: the type and content columns are read (REQ-DOC-013). */
    public static DocumentSourceQuery forBlob(String serviceCode, String connectionName, String sqlText,
                                              String inputName, String requestNumber,
                                              String documentTypeColumn, String contentColumn, int maxRows) {
        return new DocumentSourceQuery(serviceCode, connectionName, sqlText, inputName, requestNumber,
                documentTypeColumn, null, Objects.requireNonNull(contentColumn, "contentColumn"), maxRows);
    }

    /** The query's single named bind parameter, {@code :{inputName}} (CON-REG-003). */
    public String bindParameter() {
        return ":" + inputName;
    }

    /**
     * The row limit an adapter requests: {@code maxRows + 1}, so that an over-limit result is
     * detected by its size and never truncated silently (REQ-DOC-039).
     */
    public int rowLimit() {
        return maxRows + 1;
    }
}
