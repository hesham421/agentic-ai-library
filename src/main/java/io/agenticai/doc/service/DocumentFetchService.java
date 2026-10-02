package io.agenticai.doc.service;

import io.agenticai.platform.config.CheckLimitsProperties;
import io.agenticai.doc.contract.DocumentOutcome;
import io.agenticai.doc.contract.ServiceVersionNotFoundException;
import io.agenticai.doc.domain.FetchMode;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.ConnectionLookup;
import io.agenticai.doc.port.DocumentSourceQuery;
import io.agenticai.doc.port.DocumentSourceQueryPort;
import io.agenticai.doc.port.DocumentSourceRow;
import io.agenticai.doc.port.HostFilePort;
import io.agenticai.doc.port.VersionDocumentSettings;
import io.agenticai.doc.port.VersionDocumentSource;
import io.agenticai.doc.port.VersionLookup;
import io.agenticai.reg.contract.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CON-DOC-004 — the fetch and reading of the documents of a Check (SVC-API fetchDocuments,
 * steps 1–7). Honours: CON-DOC-004. Orchestration only: the version's settings from the
 * registry port, the one branch of its fetch mode (REQ-DOC-001), every document through the
 * reading step, the MISSING outcomes, and exactly one outcome per document back to the caller
 * (REQ-DOC-034, REQ-DOC-035).
 *
 * <p>Transactions: this class holds none. The host query, the host file reads and the
 * document-reading model run with no database transaction open; the only database read, the
 * {@code manual} branch, is {@link ManualUploadLoader}'s own short read-only transaction, which
 * has closed before any document is read. Nothing fetched is kept: every byte array is a local
 * of one call (REQ-DOC-055; G9). DOC calls no host endpoint (REQ-DOC-053).
 *
 * <p>The document source adapter is chosen by the version's fetch mode through
 * {@link DocumentSourceQueryPort#mode()} — no adapter is named here.
 */
@Service
public class DocumentFetchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentFetchService.class);

    private final VersionLookup versions;
    private final ConnectionLookup connections;
    private final Map<FetchMode, DocumentSourceQueryPort> sourceQueries;
    private final HostFilePort hostFiles;
    private final ManualUploadLoader uploads;
    private final DocumentReadStep reader;
    private final int maxRows;

    public DocumentFetchService(VersionLookup versions,
                                ConnectionLookup connections,
                                List<DocumentSourceQueryPort> sourceQueries,
                                HostFilePort hostFiles,
                                ManualUploadLoader uploads,
                                DocumentReadStep reader,
                                CheckLimitsProperties limits) {
        this.versions = Objects.requireNonNull(versions, "versions");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.sourceQueries = byMode(Objects.requireNonNull(sourceQueries, "sourceQueries"));
        this.hostFiles = Objects.requireNonNull(hostFiles, "hostFiles");
        this.uploads = Objects.requireNonNull(uploads, "uploads");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.maxRows = Objects.requireNonNull(limits, "limits").maxRows();
    }

    /**
     * The outcomes of the Check's documents — see {@code DocumentAccess#fetchDocuments}.
     *
     * @throws ServiceVersionNotFoundException REQ-DOC-003 — nothing is fetched
     */
    public List<DocumentOutcome> fetchDocuments(Long checkId,
                                                String requestNumber,
                                                String serviceCode,
                                                int versionNumber,
                                                Instant deadline) {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(requestNumber, "requestNumber");
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(deadline, "deadline");

        // 1 — the version's fetch mode, document source and required document types
        VersionDocumentSettings version = versions.find(serviceCode, versionNumber);
        FetchMode mode = version.fetchMode();
        String sourceMode = mode.storedValue();
        Set<String> requiredTypes = version.requiredDocumentTypes();
        log.debug("DOC fetch checkId={} serviceCode={} versionNumber={} fetchMode={} requiredTypes={}",
                checkId, serviceCode, versionNumber, sourceMode, requiredTypes.size());

        // 2, 3, 4 — only the fetch mode's branch runs (REQ-DOC-001)
        ReadOutcome<List<FetchedDocument>> fetched = switch (mode) {
            case PATH, BLOB -> fromSourceQuery(version, mode, serviceCode, requestNumber, deadline);
            case MANUAL -> new Read<>(uploads.load(checkId));
        };

        // 5, 6, 7 — or, after SOURCE_QUERY_FAILED, every required type UNREADABLE (REQ-DOC-039)
        List<DocumentOutcome> outcomes = switch (fetched) {
            case Unreadable<List<FetchedDocument>> failed -> allRequiredUnreadable(requiredTypes, sourceMode, failed);
            case Read<List<FetchedDocument>> documents -> outcomesOf(documents.value(), requiredTypes, sourceMode, deadline);
        };
        log.debug("DOC fetch done checkId={} outcomes={}", checkId, outcomes.size());
        return outcomes;
    }

    /** Steps 2 and 3: the document source query through the adapter of the fetch mode. */
    private ReadOutcome<List<FetchedDocument>> fromSourceQuery(VersionDocumentSettings version,
                                                               FetchMode mode,
                                                               String serviceCode,
                                                               String requestNumber,
                                                               Instant deadline) {
        VersionDocumentSource source = version.documentSourceQuery();
        if (source == null || version.documentTypeColumn() == null
                || (mode == FetchMode.PATH ? version.documentPathColumn() : version.documentContentColumn()) == null) {
            // the query cannot run: the documents are unknown, which is REQ-DOC-039's outcome, never MISSING
            return new Unreadable<>(UnreadableReason.SOURCE_QUERY_FAILED,
                    "the documents of \"" + serviceCode + "\" could not be fetched: the version's document source is incomplete");
        }
        ConnectionSettings connection = connections.find(source.connectionName()).orElse(null);
        DocumentSourceQuery query = mode == FetchMode.PATH
                ? DocumentSourceQuery.forPath(serviceCode, source.connectionName(), source.sqlText(), version.inputName(),
                        requestNumber, version.documentTypeColumn(), version.documentPathColumn(), maxRows)
                : DocumentSourceQuery.forBlob(serviceCode, source.connectionName(), source.sqlText(), version.inputName(),
                        requestNumber, version.documentTypeColumn(), version.documentContentColumn(), maxRows);
        return switch (sourceQueries.get(mode).run(query, connection, deadline)) {
            case Unreadable<List<DocumentSourceRow>> failed -> failed.retyped();
            case Read<List<DocumentSourceRow>> rows -> new Read<>(rows.value().stream()
                    .map(row -> fetchedOf(row, mode, deadline))
                    .toList());
        };
    }

    /** A {@code path} row's file is read by the host file port when the reading step asks (REQ-DOC-005). */
    private FetchedDocument fetchedOf(DocumentSourceRow row, FetchMode mode, Instant deadline) {
        return mode == FetchMode.PATH
                ? new FetchedDocument(row.documentType(), () -> fileOf(row.location(), deadline))
                : FetchedDocument.of(row.documentType(), row.content());
    }

    private ReadOutcome<byte[]> fileOf(String location, Instant deadline) {
        if (ReadFailures.passed(deadline)) {
            return ReadFailures.outOfTime(deadline);
        }
        return hostFiles.read(location);
    }

    /** Steps 5, 6 and 7. */
    private List<DocumentOutcome> outcomesOf(List<FetchedDocument> documents,
                                             Set<String> requiredTypes,
                                             String sourceMode,
                                             Instant deadline) {
        List<DocumentOutcome> outcomes = new ArrayList<>(documents.size() + requiredTypes.size());
        Set<String> carriedTypes = new HashSet<>();
        for (FetchedDocument document : documents) {
            outcomes.add(reader.read(document, sourceMode, deadline));
            if (document.documentType() != null) {
                carriedTypes.add(document.documentType());
            }
        }
        for (String requiredType : requiredTypes) {
            if (!carriedTypes.contains(requiredType)) {
                outcomes.add(DocumentOutcome.missing(requiredType, sourceMode));
            }
        }
        return List.copyOf(outcomes);
    }

    /** REQ-DOC-039, ADR-DOC-007: one UNREADABLE / SOURCE_QUERY_FAILED per required type, nothing else. */
    private static List<DocumentOutcome> allRequiredUnreadable(Set<String> requiredTypes,
                                                               String sourceMode,
                                                               Unreadable<?> failure) {
        return requiredTypes.stream()
                .map(type -> DocumentOutcome.unreadable(type, sourceMode, failure.reason().storedValue(), failure.detail()))
                .toList();
    }

    private static Map<FetchMode, DocumentSourceQueryPort> byMode(List<DocumentSourceQueryPort> ports) {
        Map<FetchMode, DocumentSourceQueryPort> byMode = new EnumMap<>(FetchMode.class);
        for (DocumentSourceQueryPort port : ports) {
            DocumentSourceQueryPort earlier = byMode.putIfAbsent(port.mode(), port);
            if (earlier != null) {
                throw new IllegalStateException("Two document source adapters serve fetch mode " + port.mode());
            }
        }
        for (FetchMode queried : List.of(FetchMode.PATH, FetchMode.BLOB)) {
            if (!byMode.containsKey(queried)) {
                throw new IllegalStateException("No document source adapter serves fetch mode " + queried);
            }
        }
        return Map.copyOf(byMode);
    }
}
