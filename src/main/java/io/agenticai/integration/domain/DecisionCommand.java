package io.agenticai.integration.domain;

import java.util.Objects;

/**
 * The Employee Decision as API-INT-004 received it — every value exactly as received; the
 * {@link ApprovalGuard} decides whether it is complete before any Approval API call
 * (RULE-INT-002), the Report Store otherwise (RULE-RPT-013).
 *
 * @param checkId          DBF-INT-001 — the Check identifier
 * @param employeeDecision DBF-INT-007 — the EMPLOYEE_DECISION code as received (APPROVED /
 *                         REJECTED expected, CON-RPT-002)
 * @param decidedBy        DBF-INT-008 — the deciding employee, as received
 */
public record DecisionCommand(Long checkId, String employeeDecision, String decidedBy) {

    public DecisionCommand {
        Objects.requireNonNull(checkId, "checkId");
    }
}
