package io.agenticai.reg.contract;

import java.util.Objects;

/**
 * CON-REG-011 — the settings of a registered, read-only connection of this environment
 * (REQ-REG-048, ADR-REG-009). Immutable. Holds a credential <em>reference</em> only — the name
 * under which the environment's secret store keeps the credential — never the credential
 * (REQ-REG-054).
 *
 * @param connectionName      the connection name, unique in this environment
 * @param connectionType      {@code mcp} or {@code jdbc} — the closed lookup, as its stored value
 *                            (CON-REG-005)
 * @param endpoint            the MCP server address or JDBC URL
 * @param queryTool           the MCP query tool name; {@code null} for a {@code jdbc} connection
 * @param dialect             the SQL dialect of the data source
 * @param credentialReference the name of the credential in the environment's secret store
 * @param limitedToViews      whether the read-only database user is limited to specific views
 */
public record ConnectionSettings(String connectionName,
                                 String connectionType,
                                 String endpoint,
                                 String queryTool,
                                 String dialect,
                                 String credentialReference,
                                 boolean limitedToViews) {

    public ConnectionSettings {
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(connectionType, "connectionType");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(dialect, "dialect");
        Objects.requireNonNull(credentialReference, "credentialReference");
    }
}
