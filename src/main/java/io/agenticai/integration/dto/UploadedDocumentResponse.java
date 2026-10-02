package io.agenticai.integration.dto;

import io.agenticai.doc.contract.UploadedDocumentSummary;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The API document's {@code UploadedDocumentResponse} schema (API-INT-007), field for field —
 * Document Access's summary, unchanged; never the content (REQ-INT-063).
 *
 * @param uploadedDocumentId the Uploaded Document's identifier, integer int64
 * @param documentType       the document type code, string ≤ 100
 * @param fileName           the file name as uploaded, string ≤ 255
 * @param fileSize           the file size in bytes, integer int64
 * @param oversized          {@code true} when larger than the maximum file size
 * @param uploadedAt         the upload time, ISO-8601 date-time
 */
public record UploadedDocumentResponse(Long uploadedDocumentId,
                                       String documentType,
                                       String fileName,
                                       long fileSize,
                                       boolean oversized,
                                       OffsetDateTime uploadedAt) {

    /** The one mapping of Document Access's summary to the response; no decision is taken here. */
    public static UploadedDocumentResponse of(UploadedDocumentSummary summary) {
        Objects.requireNonNull(summary, "summary");
        return new UploadedDocumentResponse(summary.uploadedDocumentId(), summary.documentType(),
                summary.fileName(), summary.fileSize(), summary.oversized(), summary.uploadedAt());
    }
}
