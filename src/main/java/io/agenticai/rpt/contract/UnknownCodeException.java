package io.agenticai.rpt.contract;

/**
 * RULE-RPT-006 — a carried code is not a value of its closed list (a creation status other than
 * RUNNING / AWAITING_DOCUMENTS counts as outside); nothing is stored (REQ-RPT-015). In-process
 * code {@value ReportRejectionCodes#UNKNOWN_CODE}.
 */
public class UnknownCodeException extends RptRefusalException {

    /**
     * @param value     the value received, as received
     * @param lookupKey the closed list it was matched against, e.g. {@code CHECK_STATUS}
     */
    public UnknownCodeException(String value, String lookupKey) {
        super(ReportRejectionCodes.UNKNOWN_CODE, ReportRejectionCodes.UNKNOWN_CODE, null, value, lookupKey);
    }
}
