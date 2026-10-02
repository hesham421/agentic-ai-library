package io.agenticai.rpt.contract;

/**
 * RULE-RPT-013 — the decision of {@code recordDecision} is not APPROVED / REJECTED, has no
 * deciding employee, or does not say whether it was executed through the Approval API; nothing is
 * recorded (REQ-RPT-035). In-process code {@value ReportRejectionCodes#DECISION_INCOMPLETE}, with
 * one of the RULE's three messages.
 */
public class DecisionIncompleteException extends RptRefusalException {

    private static final String CODE_UNKNOWN = ReportRejectionCodes.DECISION_INCOMPLETE + ".CODE-UNKNOWN";
    private static final String DECIDED_BY_MISSING = ReportRejectionCodes.DECISION_INCOMPLETE + ".DECIDED-BY-MISSING";
    private static final String APPROVAL_FLAG_MISSING = ReportRejectionCodes.DECISION_INCOMPLETE + ".APPROVAL-FLAG-MISSING";

    private DecisionIncompleteException(String messageKey, Object... arguments) {
        super(ReportRejectionCodes.DECISION_INCOMPLETE, messageKey, null, arguments);
    }

    /** "The decision was not recorded: `{value}` is not APPROVED or REJECTED." */
    public static DecisionIncompleteException codeUnknown(String value) {
        return new DecisionIncompleteException(CODE_UNKNOWN, value);
    }

    /** "The decision was not recorded: the deciding employee is missing." */
    public static DecisionIncompleteException decidedByMissing() {
        return new DecisionIncompleteException(DECIDED_BY_MISSING);
    }

    /** "The decision was not recorded: say whether it was executed through the Approval API." */
    public static DecisionIncompleteException approvalFlagMissing() {
        return new DecisionIncompleteException(APPROVAL_FLAG_MISSING);
    }
}
