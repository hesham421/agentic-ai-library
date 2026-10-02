package io.agenticai.chk.port;

import java.util.Objects;

/**
 * One run of a service query of the Check's pinned version (CON-REG-003), as the pipeline hands it
 * to the {@link ServiceQueryPort}: the query's name and connection, its SQL text exactly as stored,
 * the version's input name, the Check's request number to bind, and the platform's row limit
 * (REQ-CHK-011, REQ-CHK-012, REQ-CHK-016, REQ-CHK-050).
 *
 * <p>Facts only. The SQL text is never changed here and the request number is never put into it —
 * an adapter binds it under {@code :{inputName}} (AIAS-5; G4). The record is the Check's working
 * data and is never stored, cached or logged with its text or value (G9).
 *
 * @param queryName      the query's name — the key of its rows and of an {@link UnreadQuery}
 * @param connectionName the connection the query names — fills RULE-CHK-002's
 *                       {@code {connectionName}} even when REG could not supply its settings
 * @param sqlText        the query text, exactly as stored (REQ-CHK-011)
 * @param inputName      the version's input name: the query's only parameter is
 *                       {@code :{inputName}} (CON-REG-003)
 * @param requestNumber  the Check's request number, bound to that parameter, never concatenated
 *                       (REQ-CHK-012)
 * @param maxRows        {@code aias.check.max-rows}: more rows than this make the query not read
 *                       (REQ-CHK-050)
 */
public record ServiceQuery(String queryName,
                           String connectionName,
                           String sqlText,
                           String inputName,
                           String requestNumber,
                           int maxRows) {

    public ServiceQuery {
        Objects.requireNonNull(queryName, "queryName");
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(sqlText, "sqlText");
        Objects.requireNonNull(inputName, "inputName");
        Objects.requireNonNull(requestNumber, "requestNumber");
        if (maxRows < 1 || maxRows == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maxRows must be positive and leave room for one more row: " + maxRows);
        }
    }

    /**
     * The row limit an adapter requests: {@code maxRows + 1}, so an over-limit result is detected by
     * its size and never truncated silently (REQ-CHK-050).
     */
    public int rowLimit() {
        return maxRows + 1;
    }
}
