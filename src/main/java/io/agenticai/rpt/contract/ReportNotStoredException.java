package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * REQ-RPT-009 — a database failure while {@code completeCheck} wrote the report. In-process code
 * {@value ReportRejectionCodes#REPORT_NOT_STORED}; the database failure is kept as the cause.
 *
 * <p>Unlike every other row of the table this is not a refusal decided before the writes: the
 * write methods declare it {@code rollbackFor} (more specific than their
 * {@code noRollbackFor = RptRefusalException.class}), so it rolls the caller's transaction back —
 * no part of the report remains, OVERALL_STATUS stays NULL and the Check stays RUNNING; the Check
 * Engine then fails the Check INTERNAL_ERROR (AC-RPT-062).
 */
public class ReportNotStoredException extends RptRefusalException {

    /**
     * @param checkId the Check identifier
     * @param cause   the database failure
     */
    public ReportNotStoredException(Long checkId, Throwable cause) {
        super(ReportRejectionCodes.REPORT_NOT_STORED, ReportRejectionCodes.REPORT_NOT_STORED,
                Objects.requireNonNull(cause, "cause"), Objects.requireNonNull(checkId, "checkId"));
    }
}
