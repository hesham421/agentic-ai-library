package io.agenticai.rpt.contract;

/**
 * RULE-RPT-001 — a value of {@code createCheckRun} is absent or blank; nothing is stored
 * (REQ-RPT-003). In-process code {@value ReportRejectionCodes#CHECK_RUN_INCOMPLETE}.
 */
public class CheckRunIncompleteException extends RptRefusalException {

    /** @param field the first missing value */
    public CheckRunIncompleteException(String field) {
        super(ReportRejectionCodes.CHECK_RUN_INCOMPLETE, ReportRejectionCodes.CHECK_RUN_INCOMPLETE, null, field);
    }
}
