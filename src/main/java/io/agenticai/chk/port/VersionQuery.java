package io.agenticai.chk.port;

import java.util.Objects;

/**
 * One service query of a pinned service package version (CON-REG-003), as the
 * {@link PackageLookup} hands it to CHK: facts only, the SQL text exactly as stored (REQ-CHK-011).
 * Working data of one Check — never stored, cached or logged with its text (G9).
 *
 * @param queryName      the query's name (business key within the version)
 * @param connectionName the connection the query names
 * @param sqlText        the SQL text, unaltered
 */
public record VersionQuery(String queryName, String connectionName, String sqlText) {

    public VersionQuery {
        Objects.requireNonNull(queryName, "queryName");
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(sqlText, "sqlText");
    }
}
