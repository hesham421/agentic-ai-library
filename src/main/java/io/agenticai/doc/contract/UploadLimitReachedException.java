package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessException;
import io.agenticai.doc.error.DocumentAccessTexts;

import java.util.Objects;

/**
 * RULE-DOC-010 — the Check already holds the maximum uploads per Check
 * ({@code aias.check.max-uploads}), so the upload is rejected and nothing is stored
 * (REQ-DOC-063, ADR-DOC-016; CON-DOC-003). In-process code
 * {@value DocumentRejectionCodes#UPLOAD_LIMIT_REACHED} (ADR-DOC-012).
 */
public class UploadLimitReachedException extends DocumentAccessException {

    private final Long checkId;
    private final int maxUploads;

    /**
     * @param checkId    the Check's identifier
     * @param maxUploads the configured maximum uploads per Check
     */
    public UploadLimitReachedException(Long checkId, int maxUploads) {
        super(DocumentRejectionCodes.UPLOAD_LIMIT_REACHED,
                DocumentAccessTexts.english(DocumentRejectionCodes.UPLOAD_LIMIT_REACHED,
                        Objects.requireNonNull(checkId, "checkId"), maxUploads),
                null);
        this.checkId = checkId;
        this.maxUploads = maxUploads;
    }

    public Long checkId() {
        return checkId;
    }

    public int maxUploads() {
        return maxUploads;
    }
}
