package io.agenticai.integration.dto;

import io.agenticai.integration.domain.DecisionCommand;

/**
 * The API document's {@code DecisionRequest} schema (API-INT-004), field for field.
 * {@code approvalApiExecuted} is never accepted from the caller — INT sets it (REQ-INT-026).
 *
 * <p><b>No bean-validation constraint, by design.</b> The document marks both fields required and
 * {@code employeeDecision} an enum, but the unit (SVC-API-COMMAND, API-INT-004) assigns these
 * checks to RULE-INT-002 / the Report Store: a missing deciding employee or a code such as
 * {@code MAYBE} must answer {@code RPT-400-DECISION-INCOMPLETE} with the Report Store's text
 * (AC-INT-038, AC-INT-079 — ADR-INT-010). A {@code @NotNull} or {@code @Pattern} here would answer
 * a different code first, so the code travels as a String. Recorded as an {@code api_doc_gaps} row.
 *
 * @param employeeDecision DBF-INT-007 — EMPLOYEE_DECISION code, {@code APPROVED} | {@code REJECTED}, required
 * @param decidedBy        DBF-INT-008 — the deciding employee, exactly as the host sent it (REQ-INT-023), string ≤ 100, required
 */
public record DecisionRequest(String employeeDecision, String decidedBy) {

    /** The decision as received, for the Check {@code checkId} — the values unchanged. */
    public DecisionCommand toCommand(Long checkId) {
        return new DecisionCommand(checkId, employeeDecision, decidedBy);
    }
}
