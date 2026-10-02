package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessErrorCodes;
import io.agenticai.doc.error.DocumentAccessException;

/**
 * CON-DOC-006 — the Check identifier of a listing is absent. The one in-process refusal that IS a
 * catalog row: it carries {@value DocumentAccessErrorCodes#CHECK_ID_REQUIRED} with the catalog's
 * message, the same code API-DOC-001 answers for a missing or non-numeric {@code checkId}
 * (SVC-API listUploadedDocuments step 1, ADR-DOC-012).
 */
public class CheckIdRequiredException extends DocumentAccessException {

    public CheckIdRequiredException() {
        super(DocumentAccessErrorCodes.CHECK_ID_REQUIRED);
    }
}
