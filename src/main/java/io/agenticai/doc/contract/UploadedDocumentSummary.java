package io.agenticai.doc.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * CON-DOC-006 — one Uploaded Document of a Check as the employee sees it, never its content
 * (REQ-DOC-064, RULE-DOC-008). Immutable. DOC holds no read status: {@code oversized} is the
 * only reading-related fact (ADR-DOC-017).
 *
 * @param uploadedDocumentId the Uploaded Document's identifier (DBF-DOC-001)
 * @param documentType       the document type the employee gave (DBF-DOC-005)
 * @param fileName           the file name as uploaded (DBF-DOC-006)
 * @param fileSize           the size of the uploaded file in bytes (DBF-DOC-007)
 * @param oversized          {@code true} when larger than the maximum file size — the Check
 *                           will report it UNREADABLE / TOO_LARGE (DBF-DOC-009)
 * @param uploadedAt         the upload time (DBF-DOC-010)
 */
public record UploadedDocumentSummary(Long uploadedDocumentId,
                                      String documentType,
                                      String fileName,
                                      long fileSize,
                                      boolean oversized,
                                      OffsetDateTime uploadedAt) {

    public UploadedDocumentSummary {
        Objects.requireNonNull(uploadedDocumentId, "uploadedDocumentId");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }
}
