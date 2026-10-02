package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessException;
import io.agenticai.doc.error.DocumentAccessTexts;

/**
 * RULE-DOC-003 — the upload carries no Check identifier, no document type or a file of 0 bytes,
 * so it is rejected (REQ-DOC-022; CON-DOC-003). In-process code
 * {@value DocumentRejectionCodes#INCOMPLETE_UPLOAD} (ADR-DOC-012).
 */
public class IncompleteUploadException extends DocumentAccessException {

    public IncompleteUploadException() {
        super(DocumentRejectionCodes.INCOMPLETE_UPLOAD,
                DocumentAccessTexts.english(DocumentRejectionCodes.INCOMPLETE_UPLOAD), null);
    }
}
