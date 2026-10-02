package io.agenticai.reg.contract;

import java.util.Objects;

/**
 * One query of a service package version, exactly as the service definition writes it
 * (CON-REG-003, REQ-REG-029): one SELECT statement whose only parameter is the named bind
 * parameter of the version's input name. A consumer binds the request number to that parameter
 * and never edits the SQL text.
 *
 * @param queryName      the query name, unique within its version
 * @param connectionName the name of the connection the query runs on, resolved through
 *                       {@link ServiceRegistry#getConnection}
 * @param sqlText        the SQL text, unaltered
 */
public record QueryDefinition(String queryName, String connectionName, String sqlText) {

    public QueryDefinition {
        Objects.requireNonNull(queryName, "queryName");
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(sqlText, "sqlText");
    }
}
