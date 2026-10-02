package io.agenticai.doc.service;

import io.agenticai.doc.contract.CheckIdRequiredException;
import io.agenticai.doc.contract.UploadedDocumentSummary;
import io.agenticai.doc.repository.UploadedDocumentListing;
import io.agenticai.doc.repository.UploadedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * CON-DOC-006 — the Uploaded Documents of a Check, without content (SVC-API
 * listUploadedDocuments, steps 1 → 3; REQ-DOC-064). Honours: CON-DOC-006. READ_ONLY. The same
 * procedure the host integration calls in-process and API-DOC-001 delegates to (ADR-DOC-017):
 * QR-DOC-001, filtered on CHECK_ID (RULE-DOC-008), never selecting CONTENT; an unknown or ended
 * Check answers an empty list.
 */
@Service
@Transactional(readOnly = true)
public class UploadedDocumentQueryService {

    private static final Logger log = LoggerFactory.getLogger(UploadedDocumentQueryService.class);

    private final UploadedDocumentRepository uploads;

    public UploadedDocumentQueryService(UploadedDocumentRepository uploads) {
        this.uploads = Objects.requireNonNull(uploads, "uploads");
    }

    /**
     * The summaries of the Check's uploads, ordered by upload time — see
     * {@code DocumentAccess#listUploadedDocuments}.
     *
     * @throws CheckIdRequiredException step 1 — {@code checkId} is absent
     */
    public List<UploadedDocumentSummary> listUploadedDocuments(Long checkId) {
        // 1
        if (checkId == null) {
            throw new CheckIdRequiredException();
        }
        log.debug("DOC list uploads checkId={}", checkId);
        // 2 — QR-DOC-001; 3 — no row → empty list
        return uploads.findListingByCheckIdOrderByCreatedAtAsc(checkId).stream()
                .map(UploadedDocumentQueryService::summaryOf)
                .toList();
    }

    /** The one mapping of the QR-DOC-001 projection to the contract's summary: uploadedAt = CREATED_AT (DBF-DOC-010). */
    private static UploadedDocumentSummary summaryOf(UploadedDocumentListing row) {
        return new UploadedDocumentSummary(
                row.uploadedDocumentId(),
                row.documentType(),
                row.fileName(),
                row.fileSize(),
                Boolean.TRUE.equals(row.oversized()),
                row.createdAt());
    }
}
