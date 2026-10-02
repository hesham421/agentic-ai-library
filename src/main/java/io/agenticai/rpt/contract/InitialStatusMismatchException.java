package io.agenticai.rpt.contract;

/**
 * RULE-RPT-002 — the initial status of {@code createCheckRun} disagrees with the fetch mode;
 * nothing is stored (REQ-RPT-004). In-process code
 * {@value ReportRejectionCodes#INITIAL_STATUS_MISMATCH}, with one of the RULE's two messages.
 */
public class InitialStatusMismatchException extends RptRefusalException {

    private static final String AWAITING_NEEDS_MANUAL =
            ReportRejectionCodes.INITIAL_STATUS_MISMATCH + ".AWAITING-NEEDS-MANUAL";
    private static final String MANUAL_STARTS_AWAITING =
            ReportRejectionCodes.INITIAL_STATUS_MISMATCH + ".MANUAL-STARTS-AWAITING";

    private InitialStatusMismatchException(String messageKey) {
        super(ReportRejectionCodes.INITIAL_STATUS_MISMATCH, messageKey, null);
    }

    /** "The Check run was not stored: status AWAITING_DOCUMENTS needs fetch mode manual." */
    public static InitialStatusMismatchException awaitingNeedsManual() {
        return new InitialStatusMismatchException(AWAITING_NEEDS_MANUAL);
    }

    /** "The Check run was not stored: a manual Check starts AWAITING_DOCUMENTS." */
    public static InitialStatusMismatchException manualStartsAwaiting() {
        return new InitialStatusMismatchException(MANUAL_STARTS_AWAITING);
    }
}
