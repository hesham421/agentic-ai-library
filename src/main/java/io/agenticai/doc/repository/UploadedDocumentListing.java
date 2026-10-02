package io.agenticai.doc.repository;

import java.time.OffsetDateTime;

/**
 * One Uploaded Document of a Check as the employee sees it — never its content (REQ-DOC-064,
 * RULE-DOC-008). Projection of QR-DOC-001
 * ({@link UploadedDocumentRepository#findListingByCheckIdOrderByCreatedAtAsc}): exactly the
 * columns {@code UPLOADED_DOCUMENT_ID, DOCUMENT_TYPE, FILE_NAME, FILE_SIZE, OVERSIZED, CREATED_AT}
 * of {@code DOC_UPLOADED_DOC}; {@code CONTENT} is not selected.
 *
 * @param uploadedDocumentId DBF-DOC-001
 * @param documentType       DBF-DOC-005
 * @param fileName           DBF-DOC-006
 * @param fileSize           DBF-DOC-007
 * @param oversized          DBF-DOC-009 — the only reading-related fact DOC holds (ADR-DOC-017)
 * @param createdAt          DBF-DOC-010 — the upload time ({@code uploadedAt} of CON-DOC-006)
 */
public record UploadedDocumentListing(
        Long uploadedDocumentId,
        String documentType,
        String fileName,
        Long fileSize,
        Boolean oversized,
        OffsetDateTime createdAt) {
}
