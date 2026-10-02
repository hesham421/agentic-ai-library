package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-003 — the Check has already ended (COMPLETED or FAILED); its status cannot change (REQ-RPT-006, REQ-RPT-020). In-process code {@value ReportRejectionCodes#CHECK_ENDED}.
 */
public class CheckEndedException extends RptRefusalException {

    private final Long checkId;

    /** @param checkId the Check identifier */
    public CheckEndedException(Long checkId) {
        super(ReportRejectionCodes.CHECK_ENDED, ReportRejectionCodes.CHECK_ENDED, null,
                Objects.requireNonNull(checkId, "checkId"));
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
