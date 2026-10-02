package io.agenticai.doc.contract;

import java.util.Objects;

/**
 * CON-DOC-003 — what the handover of an upload answers: the stored Uploaded Document's identity
 * and the facts the employee sees (REQ-DOC-017, REQ-DOC-043). Immutable; never the content.
 *
 * @param uploadedDocumentId the identifier of the new Uploaded Document (DBF-DOC-001)
 * @param documentType       the document type the employee gave (DBF-DOC-005)
 * @param fileName           the file name as uploaded (DBF-DOC-006)
 * @param fileSize           the size of the uploaded file in bytes (DBF-DOC-007)
 * @param oversized          {@code true} when the file was larger than the maximum file size and
 *                           its content was not kept (DBF-DOC-009, RULE-DOC-005)
 * @param notice             the RULE-DOC-005 message when {@code oversized}; {@code null} otherwise
 */
public record UploadReceipt(Long uploadedDocumentId,
                            String documentType,
                            String fileName,
                            long fileSize,
                            boolean oversized,
                            String notice) {

    public UploadReceipt {
        Objects.requireNonNull(uploadedDocumentId, "uploadedDocumentId");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(fileName, "fileName");
        if (oversized == (notice == null)) {
            throw new IllegalArgumentException("notice is present exactly when oversized (RULE-DOC-005)");
        }
    }
}
