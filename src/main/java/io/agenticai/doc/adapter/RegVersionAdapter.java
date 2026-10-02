package io.agenticai.doc.adapter;

import io.agenticai.doc.contract.ServiceVersionNotFoundException;
import io.agenticai.doc.domain.FetchMode;
import io.agenticai.doc.port.VersionDocumentSettings;
import io.agenticai.doc.port.VersionDocumentSource;
import io.agenticai.doc.port.VersionLookup;
import io.agenticai.reg.contract.DocumentSource;
import io.agenticai.reg.contract.QueryDefinition;
import io.agenticai.reg.contract.ServicePackageVersionView;
import io.agenticai.reg.contract.ServiceRegistry;
import io.agenticai.reg.contract.VersionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * The {@link VersionLookup} over the Service Registry's published in-process interface
 * (REQ-DOC-002, REQ-DOC-003, REQ-DOC-020): the only DOC class that calls {@link ServiceRegistry}
 * for a stored version. One call to CON-REG-009 {@code getServicePackageVersion(serviceCode,
 * versionNumber)} per lookup; the answer is the exact, immutable version the pair names
 * (CON-REG-002 — never changed, never deleted, so a recorded pair always resolves).
 *
 * <p>Version pinning (ADR-REG-020): DOC always reads the Check's <em>pinned</em> version through
 * this lookup — never the registry's "current version" read — so a load run that makes another
 * version current while a Check runs cannot make DOC and CHK disagree.
 *
 * <p>Mapping, one to one into DOC's own {@link VersionDocumentSettings}:
 * <ul>
 *   <li>the stored fetch mode into DOC's {@link FetchMode} (a value outside the closed set cannot
 *       be stored — REG's CHECK constraint — so one is an integrity failure, not a caller error),
 *       the input name, and the four document-source fields of a {@code path} / {@code blob}
 *       version; a {@code manual} version has no document source and maps to {@code null}
 *       fields (REQ-DOC-002);</li>
 *   <li>the document source query (CON-REG-003; REQ-DOC-004, REQ-DOC-012, REQ-DOC-051): of the
 *       version's queries, the one whose name equals the document source's query name, exactly
 *       as stored — its SQL text unaltered and its connection name — into
 *       {@link VersionDocumentSource}; {@code null} for a {@code manual} version. A document
 *       source naming a query the version does not declare is rejected by REG's load run
 *       (RULE-REG-009) and so cannot be stored; should it still arrive, it maps to {@code null}
 *       with a warning naming the service, version and query name — never the SQL — so the fetch
 *       degrades to its recorded SOURCE_QUERY_FAILED outcome rather than failing the Check;</li>
 *   <li>the required document types (CON-REG-004; REQ-DOC-021, REQ-DOC-035): the version's
 *       values exactly as stored, case-sensitive, in declared order, into an unmodifiable set —
 *       RULE-DOC-002's data at handover and the MISSING outcomes of a fetch.</li>
 * </ul>
 * The registry's not-found refusal, which carries no code of its own, becomes DOC's documented
 * {@link ServiceVersionNotFoundException} (DOC-404-SERVICE-VERSION-NOT-FOUND) with the refusal
 * kept as cause (A.4.9, E.1.4).
 *
 * <p>Stateless: the registry's view is mapped and dropped inside the call — no copy, no field,
 * no cache (G9). The REG interface is a bean of the same deployable, injected by type.
 */
@Component
public class RegVersionAdapter implements VersionLookup {

    private static final Logger log = LoggerFactory.getLogger(RegVersionAdapter.class);

    private final ServiceRegistry registry;

    public RegVersionAdapter(ServiceRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public VersionDocumentSettings find(String serviceCode, int versionNumber) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        ServicePackageVersionView view;
        try {
            view = registry.getServicePackageVersion(serviceCode, versionNumber);
        } catch (VersionNotFoundException notFound) {
            throw new ServiceVersionNotFoundException(serviceCode, versionNumber, notFound);
        }
        return toSettings(view);
    }

    private static VersionDocumentSettings toSettings(ServicePackageVersionView view) {
        FetchMode fetchMode = FetchMode.fromStored(view.fetchMode())
                .orElseThrow(() -> new IllegalStateException(
                        "The registry supplied a fetch mode outside the closed set for version "
                                + view.versionNumber() + " of the service \"" + view.serviceCode() + "\""));
        DocumentSource source = view.documentSource();
        return new VersionDocumentSettings(
                fetchMode,
                view.inputName(),
                source == null ? null : source.documentSourceQueryName(),
                source == null ? null : source.documentTypeColumn(),
                source == null ? null : source.documentPathColumn(),
                source == null ? null : source.documentContentColumn(),
                source == null ? null : documentSourceQuery(view, source.documentSourceQueryName()),
                new LinkedHashSet<>(view.requiredDocumentTypes()));
    }

    /**
     * CON-REG-003 — the query of the version whose name equals the document source's query name,
     * matched exactly as stored; its SQL text and connection name are passed on unaltered
     * (REQ-DOC-051). {@code null}, with a warning, when the version declares no query of that name.
     */
    private static VersionDocumentSource documentSourceQuery(ServicePackageVersionView view, String queryName) {
        for (QueryDefinition query : view.queries()) {
            if (query.queryName().equals(queryName)) {
                return new VersionDocumentSource(query.sqlText(), query.connectionName());
            }
        }
        log.warn("The document source of version {} of the service \"{}\" names the query \"{}\", "
                        + "which the version does not declare; the document source query is unavailable",
                view.versionNumber(), view.serviceCode(), queryName);
        return null;
    }
}
