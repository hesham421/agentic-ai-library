package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessException;
import io.agenticai.doc.error.DocumentAccessTexts;

import java.util.Objects;

/**
 * RULE-DOC-009 — the upload's Check is recorded as an Ended Check, so the upload is rejected and
 * nothing is stored (REQ-DOC-061, ADR-DOC-015; CON-DOC-003). In-process code
 * {@value DocumentRejectionCodes#CHECK_ENDED} (ADR-DOC-012).
 */
public class CheckEndedException extends DocumentAccessException {

    private final Long checkId;

    /** @param checkId the identifier of the ended Check */
    public CheckEndedException(Long checkId) {
        super(DocumentRejectionCodes.CHECK_ENDED,
                DocumentAccessTexts.english(DocumentRejectionCodes.CHECK_ENDED,
                        Objects.requireNonNull(checkId, "checkId")),
                null);
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
