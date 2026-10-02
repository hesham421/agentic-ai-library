package io.agenticai.reg.port;

import java.util.List;

/**
 * The activation configuration of this environment as the {@link ActivationSource} reports it
 * (REQ-REG-049, ADR-REG-009): the environment name and its connection entries, exactly as
 * declared and in declared order. A port-level value — the configuration-binding type stays
 * behind the adapter.
 *
 * <p>Nothing here is validated: a duplicate name, an unknown type, a missing read-only
 * declaration or an over-length value reaches the load run, which refuses the entry and
 * records why (RULE-REG-013, RULE-REG-014, RULE-REG-015, RULE-REG-027).
 *
 * @param environmentName the environment's name, as declared ({@code null} when not declared)
 * @param connections     the declared connection entries, in declared order, duplicates kept;
 *                        unmodifiable, never {@code null}
 */
public record Activation(String environmentName, List<ConnectionEntry> connections) {

    public Activation {
        connections = connections == null ? List.of() : List.copyOf(connections);
    }

    /**
     * One declared connection (ENT-REG-005 as declared, before activation).
     *
     * @param name                the connection name
     * @param type                the declared type text ({@code mcp} or {@code jdbc} when valid)
     * @param endpoint            the endpoint
     * @param queryTool           the query tool of an {@code mcp} connection
     * @param dialect             the SQL dialect
     * @param credentialReference the name under which the environment's secret store holds the
     *                            credential — the reference only, never resolved (REQ-REG-054)
     * @param readOnly            the read-only declaration; {@code null} when not declared
     * @param limitedToViews      the limited-to-views declaration; {@code null} when not declared
     */
    public record ConnectionEntry(
            String name,
            String type,
            String endpoint,
            String queryTool,
            String dialect,
            String credentialReference,
            Boolean readOnly,
            Boolean limitedToViews) {
    }
}
