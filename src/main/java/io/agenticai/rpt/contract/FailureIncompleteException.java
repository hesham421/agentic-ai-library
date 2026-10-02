package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-010 — the failure of {@code failCheck} lacks its reason, a detail that is not blank, or
 * an end time not earlier than the start; nothing is stored (REQ-RPT-051). In-process code
 * {@value ReportRejectionCodes#FAILURE_INCOMPLETE}.
 */
public class FailureIncompleteException extends RptRefusalException {

    /**
     * @param checkId the Check identifier
     * @param field   the first missing value
     */
    public FailureIncompleteException(Long checkId, String field) {
        super(ReportRejectionCodes.FAILURE_INCOMPLETE, ReportRejectionCodes.FAILURE_INCOMPLETE, null,
                Objects.requireNonNull(checkId, "checkId"), field);
    }
}
