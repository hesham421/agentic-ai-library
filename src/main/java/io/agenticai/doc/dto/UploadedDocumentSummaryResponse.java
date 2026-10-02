package io.agenticai.doc.dto;

import io.agenticai.doc.contract.UploadedDocumentSummary;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The API document's {@code UploadedDocumentSummary} schema (API-DOC-001), field for field. A
 * plain JSON object — no envelope; never the file content (DBF-DOC-008, REQ-DOC-064). Documented
 * here in Javadoc: springdoc is not on the classpath.
 *
 * @param uploadedDocumentId DBF-DOC-001 — integer int64
 * @param documentType       DBF-DOC-005 — the DOCUMENT_TYPE code, at most 100 characters
 * @param fileName           DBF-DOC-006 — at most 255 characters
 * @param fileSize           DBF-DOC-007 — bytes, at least 1
 * @param oversized          DBF-DOC-009 — {@code true} when larger than the maximum file size
 *                           (content not kept)
 * @param createdAt          DBF-DOC-010 — the upload time, ISO-8601 date-time; the contract's
 *                           {@code uploadedAt}
 */
public record UploadedDocumentSummaryResponse(Long uploadedDocumentId,
                                              String documentType,
                                              String fileName,
                                              long fileSize,
                                              boolean oversized,
                                              OffsetDateTime createdAt) {

    /**
     * The one mapping of the contract's summary to the response (A.3.8): {@code createdAt} is the
     * summary's {@code uploadedAt}. No decision is taken here.
     */
    public static UploadedDocumentSummaryResponse of(UploadedDocumentSummary summary) {
        Objects.requireNonNull(summary, "summary");
        return new UploadedDocumentSummaryResponse(
                summary.uploadedDocumentId(),
                summary.documentType(),
                summary.fileName(),
                summary.fileSize(),
                summary.oversized(),
                summary.uploadedAt());
    }
}
