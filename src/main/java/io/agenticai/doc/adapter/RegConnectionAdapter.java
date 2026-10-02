package io.agenticai.doc.adapter;

import io.agenticai.doc.port.ConnectionLookup;
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
 * (REQ-DOC-012, REQ-DOC-014, REQ-DOC-052): the only DOC class that asks {@link ServiceRegistry}
 * for a connection. One call to CON-REG-011 {@code getConnection(connectionName)} per lookup; the
 * answer is the registry's own immutable contract record {@link ConnectionSettings} — connection
 * name, connection type ({@code mcp} | {@code jdbc}, the closed lookup of CON-REG-005), endpoint,
 * query tool, dialect, credential reference and the limited-to-views flag — which DOC's port
 * already declares as its value, so the mapping is a pass-through of the contract record: no copy,
 * no DOC-side settings type, nothing re-read or re-stated.
 *
 * <p>The read-only declaration (RULE-DOC-007): CON-REG-011 carries no {@code readOnly} field, so
 * none is mapped. Every connection REG supplies is declared read-only by REG's own schema
 * ({@code CHK_REG_CONNECTION_READ_ONLY}, RULE-REG-015 — the promise of CON-REG-005); the OPEN
 * {@code api_doc_gaps} row of this module ("document source query: platform MCP query channel
 * bean, credential secret store, and readOnly on CON-REG-011 not stated") records the gap. The
 * query adapters therefore read "declared read-only" as "REG supplied the settings"
 * ({@link DocumentSourceGuards#refuseUnlessDeclaredReadOnly}).
 *
 * <p>The registry's not-found refusal (no connection of that name is activated in this
 * environment) is not an error of the fetch: it becomes {@link Optional#empty()}, and the query
 * adapters turn the absent connection into UNREADABLE / SOURCE_QUERY_FAILED for every required
 * document type (ADR-DOC-007; RULE-DOC-006, RULE-DOC-007). Nothing of REG's escapes the port
 * (A.4.9, E.1.4); the only trace is one debug line naming the connection name.
 *
 * <p>The credential reference is passed on as the name it is (REQ-REG-054): this adapter never
 * resolves it — the environment's secret store does, by that reference, never REG — and never
 * logs or prints a credential or the settings (E.4.3).
 *
 * <p>Stateless: the registry's record is returned inside the call — no field but the registry,
 * no cache (G9). The REG interface is a bean of the same deployable, injected by type.
 */
@Component
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
            log.debug("DOC connection: \"{}\" is not activated in this environment; the document source query "
                    + "will be refused", connectionName);
            return Optional.empty();
        }
    }
}
