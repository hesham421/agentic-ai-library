package io.agenticai.chk.adapter;

import io.agenticai.chk.port.ConnectionLookup;
import io.agenticai.reg.contract.ConnectionNotFoundException;
import io.agenticai.reg.contract.ConnectionSettings;
import io.agenticai.reg.contract.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@link ConnectionLookup} over the Service Registry's published in-process interface
 * (REQ-CHK-006, REQ-CHK-013, REQ-CHK-014; CON-REG-005): the only CHK class that asks
 * {@link ServiceRegistry} for a connection. One call to CON-REG-011
 * {@code getConnection(connectionName)} per lookup; the answer is the registry's own immutable
 * contract record {@link ConnectionSettings} — connection name, connection type ({@code mcp} |
 * {@code jdbc}), endpoint, query tool, dialect, credential reference and the limited-to-views flag
 * — which CHK's port already declares as its value, so it is passed through unchanged.
 *
 * <p>The read-only declaration (RULE-CHK-002): CON-REG-011 carries no {@code readOnly} field, so
 * none is mapped. Every connection REG supplies is read-only by REG's own schema (RULE-REG-015,
 * the promise of CON-REG-005); the OPEN {@code api_doc_gaps} row of this module ("service queries:
 * platform MCP query channel, query-tool argument protocol, and readOnly on CON-REG-011 not
 * stated") records the gap. The query guard refuses an absent connection or a type other than
 * {@code mcp}.
 *
 * <p>The registry's not-found refusal is not an error of the Check: it becomes
 * {@link Optional#empty()}, and the query is refused by RULE-CHK-002 and recorded as not read.
 * A connection missing at the start is refused earlier by the package read (REQ-CHK-006,
 * CHK-422-CONNECTION-NOT-ACTIVATED). Nothing of REG's escapes the port (A.4.9, E.1.4); the only
 * trace is one debug line naming the connection name.
 *
 * <p>The credential reference is passed on as the name it is: the environment's secret store
 * resolves it, never REG and never this adapter; no credential or setting is ever logged.
 *
 * <p>Stateless: no field but the registry, no cache (G9). Named {@code chkRegConnectionAdapter}
 * because DOC has a class of the same simple name. The REG interface is a bean of the same
 * deployable, injected by type.
 */
@Component("chkRegConnectionAdapter")
public class RegConnectionAdapter implements ConnectionLookup {

    private static final Logger log = LoggerFactory.getLogger(RegConnectionAdapter.class);

    private final ServiceRegistry registry;

    public RegConnectionAdapter(ServiceRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public Optional<ConnectionSettings> find(String connectionName) {
        Objects.requireNonNull(connectionName, "connectionName");
        try {
            return Optional.of(registry.getConnection(connectionName));
        } catch (ConnectionNotFoundException notFound) {
            log.debug("CHK connection: \"{}\" is not activated in this environment; the service query "
                    + "will be refused", connectionName);
            return Optional.empty();
        }
    }
}
