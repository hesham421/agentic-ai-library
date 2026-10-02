package io.agenticai.reg.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Environment settings of the Service Registry (REG), bound from {@code aias.registry.*}.
 * They are environment configuration, never part of the deployable (ADR-REG-007, ADR-REG-009).
 *
 * <p>Only the package directory is required at binding (REQ-REG-016: packages are read from the
 * configured directory and nowhere else). Connection entries are bound loosely on purpose: a
 * duplicate name, an unknown type or a missing read-only declaration is refused by the load run
 * as a REJECTED Load Result (RULE-REG-013, RULE-REG-014, RULE-REG-015), not by start-up failing
 * before the load report can say why.
 *
 * <p>REG reads none of the {@code aias.check.*} limits (ADR-REG-019); they belong to CHK and DOC.
 *
 * @param packageDirectory the directory holding one folder per service package (ADR-REG-007)
 * @param environmentName  the name of this environment, written on every activated connection
 * @param connections      the activation configuration: the connections of this environment
 *                         (ADR-REG-009); never {@code null}, possibly empty
 * @param loadLockTimeout  how long a starting instance waits for the load lock before it serves
 *                         the registry as stored (REQ-REG-068, ADR-REG-015); default {@code PT30S}
 */
@Validated
@ConfigurationProperties(prefix = "aias.registry")
public record ServiceRegistryProperties(
        @NotNull Path packageDirectory,
        String environmentName,
        List<Connection> connections,
        Duration loadLockTimeout) {

    /** Default of {@code aias.registry.load-lock-timeout} (ADR-REG-015). */
    public static final Duration DEFAULT_LOAD_LOCK_TIMEOUT = Duration.parse("PT30S");

    public ServiceRegistryProperties {
        connections = connections == null ? List.of() : List.copyOf(connections);
        loadLockTimeout = loadLockTimeout == null ? DEFAULT_LOAD_LOCK_TIMEOUT : loadLockTimeout;
    }

    /**
     * One entry of {@code aias.registry.connections[]} (ADR-REG-009), as declared by the
     * service administrator. Fields are deliberately untyped and nullable so that an invalid
     * entry reaches the load run, which refuses it and records the reason.
     *
     * @param name                the connection name (unique per environment, RULE-REG-013)
     * @param type                {@code mcp} or {@code jdbc} (RULE-REG-014); validated by the load run
     * @param endpoint            the connection endpoint
     * @param queryTool           the query tool of an {@code mcp} connection
     * @param dialect             the SQL dialect of the data source
     * @param credentialReference the name under which the environment's secret store holds the
     *                            credential — a reference only, never the credential (REQ-REG-054)
     * @param readOnly            the administrator's read-only declaration (RULE-REG-015); absent
     *                            when not declared
     * @param limitedToViews      whether the connection is limited to views; absent when not declared
     */
    public record Connection(
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
