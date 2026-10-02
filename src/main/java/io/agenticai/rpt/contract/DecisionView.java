package io.agenticai.rpt.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The Employee Decision recorded beside a COMPLETED result (CON-RPT-003; API-RPT-001 schema
 * {@code DecisionView}).
 *
 * @param employeeDecision    DBF-RPT-015 — EMPLOYEE_DECISION code: APPROVED or REJECTED (CON-RPT-002)
 * @param decidedBy           DBF-RPT-016 — the deciding employee, exactly as sent
 * @param decidedAt           DBF-RPT-017 — RPT's recording time (ADR-RPT-009)
 * @param approvalApiExecuted DBF-RPT-018 — whether it was executed through the Approval API
 */
public record DecisionView(String employeeDecision,
                           String decidedBy,
                           OffsetDateTime decidedAt,
                           boolean approvalApiExecuted) {

    public DecisionView {
        Objects.requireNonNull(employeeDecision, "employeeDecision");
        Objects.requireNonNull(decidedBy, "decidedBy");
        Objects.requireNonNull(decidedAt, "decidedAt");
    }
}
