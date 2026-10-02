package io.agenticai.rpt.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The answer of {@link ReportStore#recordDecision} — the Check with its recorded decision
 * (CON-RPT-006, ADR-RPT-009).
 *
 * @param checkId             the Check identifier
 * @param employeeDecision    EMPLOYEE_DECISION code (CON-RPT-002)
 * @param decidedBy           the deciding employee, exactly as sent
 * @param decidedAt           RPT's recording time
 * @param approvalApiExecuted whether the decision was executed through the Approval API
 */
public record RecordedDecision(Long checkId,
                               String employeeDecision,
                               String decidedBy,
                               OffsetDateTime decidedAt,
                               boolean approvalApiExecuted) {

    public RecordedDecision {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(employeeDecision, "employeeDecision");
        Objects.requireNonNull(decidedBy, "decidedBy");
        Objects.requireNonNull(decidedAt, "decidedAt");
    }
}
