package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-007 — a finding of {@code completeCheck} has no condition, evidence or note, or an
 * unread service query has no name or detail; nothing is stored (REQ-RPT-016). In-process code
 * {@value ReportRejectionCodes#FINDING_INCOMPLETE}.
 *
 * <p>The RULE states a message for a finding only. An incomplete unread query (DATA-DOM ENT-RPT-004
 * applies RULE-RPT-007's not-blank guard to it) carries the same code with the plan's generic text
 * "The report of Check {checkId} was not stored." — recorded as an {@code api_doc_gaps} row, no
 * text invented.
 */
public class FindingIncompleteException extends RptRefusalException {

    private FindingIncompleteException(String messageKey, Object... arguments) {
        super(ReportRejectionCodes.FINDING_INCOMPLETE, messageKey, null, arguments);
    }

    /** "The report of Check {checkId} was not stored: finding {position} has no {field}." */
    public static FindingIncompleteException finding(Long checkId, String position, String field) {
        return new FindingIncompleteException(ReportRejectionCodes.FINDING_INCOMPLETE,
                Objects.requireNonNull(checkId, "checkId"), position, field);
    }

    /** An unread query without a name or a detail — the plan states no message of its own (gap). */
    public static FindingIncompleteException unreadQuery(Long checkId) {
        return new FindingIncompleteException(ReportRejectionCodes.REPORT_NOT_STORED,
                Objects.requireNonNull(checkId, "checkId"));
    }
}
