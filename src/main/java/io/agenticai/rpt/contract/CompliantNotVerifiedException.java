package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-005 — the report claims COMPLIANT although a finding is not SATISFIED or a service query is unread; nothing is stored (REQ-RPT-014). In-process code {@value ReportRejectionCodes#COMPLIANT_NOT_VERIFIED}.
 */
public class CompliantNotVerifiedException extends RptRefusalException {

    private final Long checkId;

    /** @param checkId the Check identifier */
    public CompliantNotVerifiedException(Long checkId) {
        super(ReportRejectionCodes.COMPLIANT_NOT_VERIFIED, ReportRejectionCodes.COMPLIANT_NOT_VERIFIED, null,
                Objects.requireNonNull(checkId, "checkId"));
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
