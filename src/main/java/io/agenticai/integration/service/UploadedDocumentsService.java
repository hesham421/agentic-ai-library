package io.agenticai.integration.service;

import io.agenticai.integration.dto.UploadedDocumentResponse;
import io.agenticai.integration.port.DocumentAccessPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * API-INT-007 — the uploaded documents of a Check (CON-INT-007; ADR-INT-020, ADR-INT-023): Document
 * Access's list relayed unchanged, in upload order, never the content (REQ-INT-063); empty for a
 * Check with no upload. Keeps nothing (REQ-INT-059).
 *
 * <p>No {@code @Transactional}: INT owns no table; Document Access's read is its own.
 */
@Service("intUploadedDocumentsService")
public class UploadedDocumentsService {

    private static final Logger log = LoggerFactory.getLogger(UploadedDocumentsService.class);

    private final DocumentAccessPort documentAccess;

    public UploadedDocumentsService(DocumentAccessPort documentAccess) {
        this.documentAccess = Objects.requireNonNull(documentAccess, "documentAccess");
    }

    /** The uploaded documents of the Check {@code checkId}. */
    public List<UploadedDocumentResponse> list(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        List<UploadedDocumentResponse> documents = documentAccess.listUploaded(checkId).stream()
                .map(UploadedDocumentResponse::of)
                .toList();
        log.debug("INT read uploaded documents checkId={} returned={}", checkId, documents.size());
        return documents;
    }
}
