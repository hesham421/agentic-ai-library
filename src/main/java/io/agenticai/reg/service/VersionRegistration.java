package io.agenticai.reg.service;

import io.agenticai.reg.domain.FetchMode;
import io.agenticai.reg.domain.LoadOutcome;
import io.agenticai.reg.domain.LoadSubject;
import io.agenticai.reg.domain.ParsedServiceDefinition;
import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedDocuments;
import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedQuery;
import io.agenticai.reg.entity.RequiredDocument;
import io.agenticai.reg.entity.ServicePackage;
import io.agenticai.reg.entity.ServicePackageVersion;
import io.agenticai.reg.entity.ServiceQuery;
import io.agenticai.reg.repository.RequiredDocumentRepository;
import io.agenticai.reg.repository.ServicePackageRepository;
import io.agenticai.reg.repository.ServicePackageVersionRepository;
import io.agenticai.reg.repository.ServiceQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.agenticai.reg.service.LoadReasonCodes.VERSION_EDITED_IN_PLACE;
import static io.agenticai.reg.service.LoadReasonCodes.VERSION_OLDER_THAN_CURRENT;

/**
 * Load run step 4 — version registration of one accepted folder (REQ-REG-006, REQ-REG-019 …
 * REQ-REG-023, ADR-REG-003): an unknown service code registers the package and its first
 * version ({@code REGISTERED}); a version number above the current one is inserted and becomes
 * current ({@code REGISTERED}); a stored version number with the same content hash changes
 * nothing ({@code UNCHANGED}); a stored number with a different hash is RULE-REG-003; a lower,
 * unstored number is RULE-REG-004. A version insert writes {@code REG_SVC_PKG_VER} with its
 * {@code REG_SVC_QUERY} and {@code REG_REQ_DOC} rows (document types as declared, in order);
 * nothing stored is ever updated or deleted (REQ-REG-026). Runs behind the folder's savepoint;
 * records the folder's one Load Result row.
 */
@Component
class VersionRegistration {

    private static final Logger log = LoggerFactory.getLogger(VersionRegistration.class);

    private final ServicePackageRepository packages;
    private final ServicePackageVersionRepository versions;
    private final ServiceQueryRepository queries;
    private final RequiredDocumentRepository requiredDocuments;
    private final LoadResultRecorder recorder;

    VersionRegistration(ServicePackageRepository packages,
                        ServicePackageVersionRepository versions,
                        ServiceQueryRepository queries,
                        RequiredDocumentRepository requiredDocuments,
                        LoadResultRecorder recorder) {
        this.packages = Objects.requireNonNull(packages, "packages");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.queries = Objects.requireNonNull(queries, "queries");
        this.requiredDocuments = Objects.requireNonNull(requiredDocuments, "requiredDocuments");
        this.recorder = Objects.requireNonNull(recorder, "recorder");
    }

    /**
     * @return {@code true} when the folder was accepted ({@code REGISTERED} or {@code UNCHANGED}),
     *         so its service code may be restored in step 5
     */
    boolean register(OffsetDateTime loadRunAt, FolderVerdict verdict, LoadRunTally tally) {
        if (verdict.rejected() || verdict.definition() == null) {
            throw new IllegalArgumentException("Only an accepted folder is registered: " + verdict.folderName());
        }
        String code = verdict.canonicalCode();
        ParsedServiceDefinition definition = verdict.definition();
        int declared = definition.versionNumber();
        String hash = ContentHash.of(verdict.knowledgeText(), verdict.definitionText());

        Optional<ServicePackage> stored = packages.findByServiceCode(code);
        if (stored.isEmpty()) {
            ServicePackage registered = packages.save(ServicePackage.register(code, loadRunAt));
            insertVersion(registered.getServicePackageId(), verdict, hash, loadRunAt);
            record(loadRunAt, verdict, LoadOutcome.REGISTERED, null);
            tally.registered++;
            log.info("REG registered service \"{}\" version {}", code, declared);
            return true;
        }
        Long packageId = stored.get().getServicePackageId();
        Optional<ServicePackageVersion> current = versions.findFirstByServicePackageIdOrderByVersionNumberDesc(packageId);
        if (current.isEmpty() || declared > current.get().getVersionNumber()) {
            insertVersion(packageId, verdict, hash, loadRunAt);
            record(loadRunAt, verdict, LoadOutcome.REGISTERED, null);
            tally.registered++;
            log.info("REG registered service \"{}\" version {} (now current)", code, declared);
            return true;
        }
        int currentNumber = current.get().getVersionNumber();
        Optional<ServicePackageVersion> sameNumber = declared == currentNumber
                ? current
                : versions.findByServicePackageIdAndVersionNumber(packageId, declared);
        if (sameNumber.isPresent()) {
            if (hash.equals(sameNumber.get().getContentHash())) {
                record(loadRunAt, verdict, LoadOutcome.UNCHANGED, null);
                tally.unchanged++;
                return true;
            }
            record(loadRunAt, verdict, LoadOutcome.REJECTED, LoadReason.of(VERSION_EDITED_IN_PLACE, declared, code));
            tally.packagesRejected++;
            return false;
        }
        record(loadRunAt, verdict, LoadOutcome.REJECTED, LoadReason.of(VERSION_OLDER_THAN_CURRENT, declared, code, currentNumber));
        tally.packagesRejected++;
        return false;
    }

    private void insertVersion(Long packageId, FolderVerdict verdict, String hash, OffsetDateTime loadRunAt) {
        ParsedServiceDefinition definition = verdict.definition();
        ParsedDocuments documents = definition.documents();
        FetchMode fetchMode = FetchMode.fromStored(documents.fetchMode()).orElseThrow();
        boolean approvalEnabled = definition.approval() != null && Boolean.TRUE.equals(definition.approval().enabled());
        String approvalApi = definition.approval() == null ? null : definition.approval().api();

        ServicePackageVersion version = versions.save(ServicePackageVersion.loaded(
                packageId,
                definition.versionNumber(),
                verdict.knowledgeText(),
                verdict.definitionText(),
                definition.inputName(),
                fetchMode,
                documents.sourceQueryName(),
                documents.typeColumn(),
                documents.pathColumn(),
                documents.contentColumn(),
                approvalEnabled,
                approvalApi,
                hash,
                loadRunAt));
        Long versionId = version.getServicePackageVersionId();

        List<ServiceQuery> queryRows = new ArrayList<>(definition.queries().size());
        for (ParsedQuery query : definition.queries()) {
            queryRows.add(ServiceQuery.of(versionId, query.queryName(), query.connectionName(), query.sqlText()));
        }
        queries.saveAll(queryRows);

        List<RequiredDocument> documentRows = new ArrayList<>(documents.requiredDocumentTypes().size());
        for (String type : documents.requiredDocumentTypes()) {
            documentRows.add(RequiredDocument.of(versionId, type));
        }
        requiredDocuments.saveAll(documentRows);
    }

    private void record(OffsetDateTime loadRunAt, FolderVerdict verdict, LoadOutcome outcome, LoadReason reason) {
        recorder.record(loadRunAt, LoadSubject.SERVICE_PACKAGE, verdict.folderName(), verdict.canonicalCode(),
                verdict.definition().versionNumber(), outcome, reason);
    }
}
