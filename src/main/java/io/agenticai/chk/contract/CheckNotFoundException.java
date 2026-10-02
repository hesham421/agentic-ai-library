package io.agenticai.chk.contract;

import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.error.CheckEngineTexts;

import java.util.Objects;

/**
 * REQ-CHK-058 — the uploads are confirmed for a Check identifier the result port does not know
 * (CON-CHK-005). In-process code {@value CheckRejectionCodes#CHECK_NOT_FOUND} (ADR-CHK-018).
 */
public class CheckNotFoundException extends CheckEngineException {

    private final Long checkId;

    /** @param checkId the unknown Check identifier */
    public CheckNotFoundException(Long checkId) {
        super(CheckRejectionCodes.CHECK_NOT_FOUND,
                CheckEngineTexts.english(CheckRejectionCodes.CHECK_NOT_FOUND,
                        Objects.requireNonNull(checkId, "checkId")),
                null);
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
