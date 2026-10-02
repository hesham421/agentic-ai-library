package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-011 — the Check already has an Employee Decision; nothing changes (REQ-RPT-033). In-process code {@value ReportRejectionCodes#DECISION_ALREADY_RECORDED}.
 */
public class DecisionAlreadyRecordedException extends RptRefusalException {

    private final Long checkId;

    /** @param checkId the Check identifier */
    public DecisionAlreadyRecordedException(Long checkId) {
        super(ReportRejectionCodes.DECISION_ALREADY_RECORDED, ReportRejectionCodes.DECISION_ALREADY_RECORDED, null,
                Objects.requireNonNull(checkId, "checkId"));
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
