package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-003 — the Check is AWAITING_DOCUMENTS, not RUNNING; it cannot be completed (REQ-RPT-006). In-process code {@value ReportRejectionCodes#CHECK_NOT_RUNNING}.
 */
public class CheckNotRunningException extends RptRefusalException {

    private final Long checkId;

    /** @param checkId the Check identifier */
    public CheckNotRunningException(Long checkId) {
        super(ReportRejectionCodes.CHECK_NOT_RUNNING, ReportRejectionCodes.CHECK_NOT_RUNNING, null,
                Objects.requireNonNull(checkId, "checkId"));
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
