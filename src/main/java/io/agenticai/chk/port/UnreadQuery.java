package io.agenticai.chk.port;

import java.util.Objects;

/**
 * A service query whose data was not read, as it is carried to the report through the Check
 * result port (ADR-CHK-014): the query name and a detail text — the query error, the row limit
 * exceeded, or the guard that refused it. No closed list of failure codes in v1. Lives in
 * {@code chk.port} beside {@link QueryResult}, as the value the result port hands on.
 *
 * @param queryName the query's name
 * @param detail    why it was not read — never the SQL text, a credential or row data
 */
public record UnreadQuery(String queryName, String detail) {

    public UnreadQuery {
        Objects.requireNonNull(queryName, "queryName");
        Objects.requireNonNull(detail, "detail");
    }
}
