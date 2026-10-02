package io.agenticai.chk.adapter;

import io.agenticai.chk.contract.ConnectionNotActivatedException;
import io.agenticai.chk.contract.ServiceNotAvailableException;
import io.agenticai.chk.port.PackageLookup;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.port.VersionQuery;
import io.agenticai.reg.contract.DocumentSource;
import io.agenticai.reg.contract.QueryDefinition;
import io.agenticai.reg.contract.ServiceConnectionNotActivatedException;
import io.agenticai.reg.contract.ServicePackageVersionView;
import io.agenticai.reg.contract.ServicePackageView;
import io.agenticai.reg.contract.ServiceRegistry;
import io.agenticai.reg.contract.VersionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link PackageLookup} over the Service Registry's published in-process interface
 * (REQ-CHK-007, REQ-CHK-008, REQ-CHK-011, REQ-CHK-015, REQ-CHK-019, REQ-CHK-034, REQ-CHK-057;
 * CON-REG-002, CON-REG-003, CON-REG-004): the only CHK class that asks {@link ServiceRegistry} for
 * a service package version.
 *
 * <p>Version pinning (ADR-REG-020): a Check's start resolves the version current at that moment
 * through CON-REG-007 {@code getCurrentServicePackage(serviceCode)}; a resumed {@code manual}
 * Check reads exactly its pinned version through CON-REG-009
 * {@code getServicePackageVersion(serviceCode, versionNumber)} (RULE-CHK-007) — never the current
 * read again. The approval API operation (CON-REG-012) is never called by CHK (REQ-CHK-032).
 *
 * <p>Mapping, one to one and identical for both registry views, into CHK's immutable
 * {@link ServicePackageSnapshot}:
 * <ul>
 *   <li>CON-REG-002 — the service code and version number (the pair travels by value, no foreign
 *       key), the whole service knowledge unaltered (RULE-CHK-006 data source), the input name
 *       (RULE-CHK-003 data source), the fetch mode as its stored code, and the document source
 *       query name — {@code null} for a {@code manual} version, which has no document source
 *       (RULE-CHK-005 data source);</li>
 *   <li>CON-REG-003 — every query of the version, in its stored order, into a
 *       {@link VersionQuery} of query name, connection name and SQL text, the SQL exactly as
 *       stored: the request number is bound to the input name downstream and the document source
 *       query is skipped by RULE-CHK-005 there, not here;</li>
 *   <li>CON-REG-004 — the required document types exactly as stored (case-sensitive), in declared
 *       order, into an unmodifiable set (RULE-CHK-004 data source); never hardcoded.</li>
 * </ul>
 *
 * <p>The registry's refusals become CHK's own documented refusals, the registry's exception kept
 * as cause (A.4.9, E.1.4): "service not available" → {@link ServiceNotAvailableException}
 * (CHK-422-SERVICE-NOT-AVAILABLE); "connection not activated" →
 * {@link ConnectionNotActivatedException} (CHK-422-CONNECTION-NOT-ACTIVATED, REQ-CHK-006) naming
 * the service and the connection; "not found" on resume → {@link Optional#empty()}, and the
 * pipeline ends the Check FAILED / INTERNAL_ERROR with RULE-CHK-007's message.
 *
 * <p>Stateless: the registry's view is mapped and dropped inside the call — no field but the
 * registry, no cache (G9). Neither the knowledge nor the SQL is ever logged. The REG interface is a
 * bean of the same deployable, injected by type.
 */
@Component("chkRegPackageAdapter")
public class RegPackageAdapter implements PackageLookup {

    private static final Logger log = LoggerFactory.getLogger(RegPackageAdapter.class);

    private final ServiceRegistry registry;

    public RegPackageAdapter(ServiceRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public ServicePackageSnapshot currentPackage(String serviceCode) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        ServicePackageView view;
        try {
            view = registry.getCurrentServicePackage(serviceCode);
        } catch (io.agenticai.reg.contract.ServiceNotAvailableException notAvailable) {
            throw new ServiceNotAvailableException(serviceCode, notAvailable);
        } catch (ServiceConnectionNotActivatedException notActivated) {
            throw new ConnectionNotActivatedException(serviceCode, notActivated.connectionName(), notActivated);
        }
        return toSnapshot(view);
    }

    @Override
    public Optional<ServicePackageSnapshot> pinnedPackage(String serviceCode, int versionNumber) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        try {
            return Optional.of(toSnapshot(registry.getServicePackageVersion(serviceCode, versionNumber)));
        } catch (VersionNotFoundException notFound) {
            log.debug("CHK package: version {} of the service \"{}\" is not stored in the registry",
                    versionNumber, serviceCode);
            return Optional.empty();
        }
    }

    private static ServicePackageSnapshot toSnapshot(ServicePackageView view) {
        return snapshot(view.serviceCode(), view.versionNumber(), view.serviceKnowledge(), view.inputName(),
                view.fetchMode(), view.documentSource(), view.queries(), view.requiredDocumentTypes());
    }

    private static ServicePackageSnapshot toSnapshot(ServicePackageVersionView view) {
        return snapshot(view.serviceCode(), view.versionNumber(), view.serviceKnowledge(), view.inputName(),
                view.fetchMode(), view.documentSource(), view.queries(), view.requiredDocumentTypes());
    }

    private static ServicePackageSnapshot snapshot(String serviceCode,
                                                   int versionNumber,
                                                   String serviceKnowledge,
                                                   String inputName,
                                                   String fetchMode,
                                                   DocumentSource documentSource,
                                                   List<QueryDefinition> queries,
                                                   List<String> requiredDocumentTypes) {
        return new ServicePackageSnapshot(
                serviceCode,
                versionNumber,
                serviceKnowledge,
                inputName,
                fetchMode,
                documentSource == null ? null : documentSource.documentSourceQueryName(),
                toQueries(queries),
                Collections.unmodifiableSet(new LinkedHashSet<>(requiredDocumentTypes)));
    }

    /** CON-REG-003 — the version's queries in their stored order, the SQL text unaltered. */
    private static List<VersionQuery> toQueries(List<QueryDefinition> queries) {
        List<VersionQuery> mapped = new ArrayList<>(queries.size());
        for (QueryDefinition query : queries) {
            mapped.add(new VersionQuery(query.queryName(), query.connectionName(), query.sqlText()));
        }
        return Collections.unmodifiableList(mapped);
    }
}
