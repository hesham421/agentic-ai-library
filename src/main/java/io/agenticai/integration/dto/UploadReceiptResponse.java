package io.agenticai.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.agenticai.doc.contract.UploadReceipt;

import java.util.Objects;

/**
 * The API document's {@code UploadReceiptResponse} schema (API-INT-002, 201), field for field —
 * Document Access's receipt, unchanged. A plain JSON object; never the file content.
 *
 * @param uploadedDocumentId the new Uploaded Document's identifier, integer int64
 * @param documentType       the document type code, string ≤ 100
 * @param fileName           the file name as uploaded, string ≤ 255
 * @param fileSize           the file size in bytes, integer int64
 * @param oversized          {@code true} when larger than the maximum file size (REQ-INT-013)
 * @param notice             present only when {@code oversized} — the file will be reported
 *                           unreadable (REQ-INT-013); omitted from the JSON otherwise
 */
public record UploadReceiptResponse(Long uploadedDocumentId,
                                    String documentType,
                                    String fileName,
                                    long fileSize,
                                    boolean oversized,
                                    @JsonInclude(JsonInclude.Include.NON_NULL) String notice) {

    /** The one mapping of Document Access's receipt to the response; no decision is taken here. */
    public static UploadReceiptResponse of(UploadReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        return new UploadReceiptResponse(receipt.uploadedDocumentId(), receipt.documentType(),
                receipt.fileName(), receipt.fileSize(), receipt.oversized(), receipt.notice());
    }
}
