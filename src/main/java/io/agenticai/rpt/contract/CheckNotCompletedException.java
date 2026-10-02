package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-012 — a decision can only be recorded on a COMPLETED Check (REQ-RPT-034). In-process code {@value ReportRejectionCodes#CHECK_NOT_COMPLETED}.
 */
public class CheckNotCompletedException extends RptRefusalException {

    private final Long checkId;

    /** @param checkId the Check identifier */
    public CheckNotCompletedException(Long checkId) {
        super(ReportRejectionCodes.CHECK_NOT_COMPLETED, ReportRejectionCodes.CHECK_NOT_COMPLETED, null,
                Objects.requireNonNull(checkId, "checkId"));
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
