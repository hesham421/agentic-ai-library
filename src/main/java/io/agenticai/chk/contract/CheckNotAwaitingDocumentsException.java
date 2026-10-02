package io.agenticai.chk.contract;

import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.error.CheckEngineTexts;

import java.util.Objects;

/**
 * RULE-CHK-010 — the uploads are confirmed for a Check whose status is not AWAITING_DOCUMENTS; the
 * Check is left unchanged (REQ-CHK-059, CON-CHK-005). In-process code
 * {@value CheckRejectionCodes#CHECK_NOT_AWAITING_DOCUMENTS} (ADR-CHK-018).
 */
public class CheckNotAwaitingDocumentsException extends CheckEngineException {

    private final Long checkId;
    private final String checkStatus;

    /**
     * @param checkId the Check identifier
     * @param status  the Check's status code (CON-CHK-001), as the message shows it
     */
    public CheckNotAwaitingDocumentsException(Long checkId, String status) {
        super(CheckRejectionCodes.CHECK_NOT_AWAITING_DOCUMENTS,
                CheckEngineTexts.english(CheckRejectionCodes.CHECK_NOT_AWAITING_DOCUMENTS,
                        Objects.requireNonNull(checkId, "checkId"),
                        Objects.requireNonNull(status, "status")),
                null);
        this.checkId = checkId;
        this.checkStatus = status;
    }

    public Long checkId() {
        return checkId;
    }

    public String checkStatus() {
        return checkStatus;
    }
}
