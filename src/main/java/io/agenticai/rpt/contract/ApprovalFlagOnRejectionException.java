package io.agenticai.rpt.contract;

/**
 * RULE-RPT-014 — a REJECTED decision handed over as executed through the Approval API; nothing is
 * recorded (REQ-RPT-039). In-process code {@value ReportRejectionCodes#APPROVAL_FLAG_ON_REJECTION}.
 */
public class ApprovalFlagOnRejectionException extends RptRefusalException {

    public ApprovalFlagOnRejectionException() {
        super(ReportRejectionCodes.APPROVAL_FLAG_ON_REJECTION, ReportRejectionCodes.APPROVAL_FLAG_ON_REJECTION, null);
    }
}
