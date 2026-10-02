package io.agenticai.doc.service;

import io.agenticai.platform.config.CheckLimitsProperties;
import io.agenticai.doc.contract.CheckEndedException;
import io.agenticai.doc.contract.DocumentRejectionCodes;
import io.agenticai.doc.contract.DocumentTypeNotOfServiceException;
import io.agenticai.doc.contract.FetchModeNotManualException;
import io.agenticai.doc.contract.IncompleteUploadException;
import io.agenticai.doc.contract.ServiceVersionNotFoundException;
import io.agenticai.doc.contract.UploadLimitReachedException;
import io.agenticai.doc.contract.UploadReceipt;
import io.agenticai.doc.domain.FetchMode;
import io.agenticai.doc.entity.UploadedDocument;
import io.agenticai.doc.error.DocumentAccessTexts;
import io.agenticai.doc.port.VersionDocumentSettings;
import io.agenticai.doc.port.VersionLookup;
import io.agenticai.doc.repository.EndedCheckRepository;
import io.agenticai.doc.repository.UploadedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import java.util.Objects;

/**
 * CON-DOC-003 — the handover of a file uploaded for a Check (SVC-API handOverUpload, steps
 * 1 → 1a → 2 → 3 → 4 → 5 → 5a → 6). Honours: CON-DOC-003. One READ_WRITE transaction; every
 * rule is checked before the one write, so a refusal leaves nothing stored. The registry read of
 * step 2 runs inside that transaction because the unit orders it between the Ended Check read
 * (1a) and the count (5a) — an in-process, in-memory read of the same deployable, not a host or
 * model call.
 *
 * <p>Rules owned here (the unit assigns them to the in-process handover; no domain class
 * exists for them): RULE-DOC-003, RULE-DOC-009, RULE-DOC-001, RULE-DOC-002, RULE-DOC-005,
 * RULE-DOC-010, in that order. Every handover is a new row (REQ-DOC-023). Concurrency:
 * OPTIMISTIC-BY-DESIGN — steps 1a and 5a read committed rows; a late row is swept by the next
 * end of a Check (ADR-DOC-015, ADR-DOC-016).
 */
@Service
@Transactional
public class UploadHandoverService {

    private static final Logger log = LoggerFactory.getLogger(UploadHandoverService.class);

    private static final long KILOBYTE = 1024L;
    private static final long MEGABYTE = KILOBYTE * 1024L;
    private static final long GIGABYTE = MEGABYTE * 1024L;

    private final UploadedDocumentRepository uploads;
    private final EndedCheckRepository endedChecks;
    private final VersionLookup versions;
    private final CheckLimitsProperties limits;

    public UploadHandoverService(UploadedDocumentRepository uploads,
                                 EndedCheckRepository endedChecks,
                                 VersionLookup versions,
                                 CheckLimitsProperties limits) {
        this.uploads = Objects.requireNonNull(uploads, "uploads");
        this.endedChecks = Objects.requireNonNull(endedChecks, "endedChecks");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Hands over one upload — see {@code DocumentAccess#handOverUpload}.
     *
     * @throws IncompleteUploadException         step 1, RULE-DOC-003
     * @throws CheckEndedException               step 1a, RULE-DOC-009
     * @throws ServiceVersionNotFoundException   step 2, REQ-DOC-003
     * @throws FetchModeNotManualException       step 3, RULE-DOC-001
     * @throws DocumentTypeNotOfServiceException step 4, RULE-DOC-002
     * @throws UploadLimitReachedException       step 5a, RULE-DOC-010
     */
    public UploadReceipt handOverUpload(Long checkId,
                                        String serviceCode,
                                        int versionNumber,
                                        String documentType,
                                        String fileName,
                                        byte[] bytes) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(fileName, "fileName");

        // 1 — RULE-DOC-003 (REQ-DOC-022)
        if (checkId == null || documentType == null || documentType.isBlank() || bytes == null || bytes.length == 0) {
            throw new IncompleteUploadException();
        }
        // 1a — RULE-DOC-009 (REQ-DOC-061, ADR-DOC-015)
        if (endedChecks.existsByCheckId(checkId)) {
            throw new CheckEndedException(checkId);
        }
        // 2 — the version, through the registry port (REQ-DOC-003 propagates)
        VersionDocumentSettings version = versions.find(serviceCode, versionNumber);
        // 3 — RULE-DOC-001 (REQ-DOC-020)
        if (version.fetchMode() != FetchMode.MANUAL) {
            throw new FetchModeNotManualException(serviceCode, version.fetchMode().storedValue());
        }
        // 4 — RULE-DOC-002 (REQ-DOC-021): compared exactly as stored
        if (!version.requiredDocumentTypes().contains(documentType)) {
            throw new DocumentTypeNotOfServiceException(documentType, serviceCode, version.requiredDocumentTypes());
        }
        // 5 — RULE-DOC-005 (REQ-DOC-043): oversized → no content, the notice
        DataSize maxFileSize = limits.maxFileSize();
        boolean oversized = bytes.length > maxFileSize.toBytes();
        String notice = oversized
                ? DocumentAccessTexts.english(DocumentRejectionCodes.OVERSIZED_UPLOAD_NOTICE, fileName, describe(maxFileSize))
                : null;
        // 5a — RULE-DOC-010 (REQ-DOC-063, ADR-DOC-016): oversized rows count
        if (uploads.countByCheckId(checkId) >= limits.maxUploads()) {
            throw new UploadLimitReachedException(checkId, limits.maxUploads());
        }
        // 6 — persist (REQ-DOC-017); a new row every time (REQ-DOC-023)
        UploadedDocument stored = uploads.save(UploadedDocument.handedOver(
                checkId, serviceCode, versionNumber, documentType, fileName,
                (long) bytes.length, oversized ? null : bytes, oversized));
        log.info("DOC upload handed over uploadedDocumentId={} checkId={} documentType={} fileSize={} oversized={}",
                stored.getUploadedDocumentId(), checkId, documentType, bytes.length, oversized);
        return new UploadReceipt(stored.getUploadedDocumentId(), documentType, fileName, bytes.length, oversized, notice);
    }

    /** The configured maximum as the RULE-DOC-005 message shows it: {@code 10MB}, {@code 512KB}, {@code 100B}. */
    private static String describe(DataSize size) {
        long bytes = size.toBytes();
        if (bytes % GIGABYTE == 0) {
            return (bytes / GIGABYTE) + "GB";
        }
        if (bytes % MEGABYTE == 0) {
            return (bytes / MEGABYTE) + "MB";
        }
        if (bytes % KILOBYTE == 0) {
            return (bytes / KILOBYTE) + "KB";
        }
        return bytes + "B";
    }
}
