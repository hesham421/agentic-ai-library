package io.agenticai.integration.domain;

import io.agenticai.rpt.contract.CheckNotCompletedException;
import io.agenticai.rpt.contract.DecisionAlreadyRecordedException;
import io.agenticai.rpt.contract.DecisionIncompleteException;

import java.util.Objects;

/**
 * RULE-INT-002 and RULE-INT-003 — checked on the approval path of API-INT-004 before the host
 * Approval API is called (REQ-INT-034, REQ-INT-035). Per ADR-INT-010 INT raises the Report Store's
 * OWN codes and texts, so the refusal is the same whether INT or the Report Store decides it: the
 * guard throws the Report Store's constructible contract exceptions
 * ({@link DecisionIncompleteException}, {@link CheckNotCompletedException},
 * {@link DecisionAlreadyRecordedException}), which carry {@code RPT-400-DECISION-INCOMPLETE},
 * {@code RPT-409-CHECK-NOT-COMPLETED} and {@code RPT-409-DECISION-ALREADY-RECORDED} with their
 * {@code messages.properties} texts. Framework-free; built through
 * {@link #forDecision(CheckSnapshot, DecisionCommand)}.
 *
 * <p>Order of the checks: the request's completeness (RULE-INT-002 — decision code, then deciding
 * employee), then the Check's state (RULE-INT-003 — not COMPLETED, then already decided).
 */
public final class ApprovalGuard {

    /** The Check Engine's status code of a completed Check (CON-CHK-001; ADR-INT-013). */
    static final String COMPLETED = "COMPLETED";

    /** The Report Store's EMPLOYEE_DECISION codes (CON-RPT-002; ADR-INT-013). */
    static final String APPROVED = "APPROVED";
    static final String REJECTED = "REJECTED";

    private final CheckSnapshot check;
    private final DecisionCommand decision;

    private ApprovalGuard(CheckSnapshot check, DecisionCommand decision) {
        this.check = check;
        this.decision = decision;
    }

    /** The guard of {@code decision} on {@code check}. */
    public static ApprovalGuard forDecision(CheckSnapshot check, DecisionCommand decision) {
        return new ApprovalGuard(
                Objects.requireNonNull(check, "check"),
                Objects.requireNonNull(decision, "decision"));
    }

    /**
     * RULE-INT-002 then RULE-INT-003; returns normally when the Approval API may be called.
     *
     * @throws DecisionIncompleteException      the decision code is not APPROVED / REJECTED, or the
     *                                          deciding employee is missing
     * @throws CheckNotCompletedException       the Check is not COMPLETED
     * @throws DecisionAlreadyRecordedException the Check already holds an Employee Decision
     */
    public void requireApprovalAllowed() {
        String code = decision.employeeDecision();
        if (!APPROVED.equals(code) && !REJECTED.equals(code)) {
            throw DecisionIncompleteException.codeUnknown(code);
        }
        String decidedBy = decision.decidedBy();
        if (decidedBy == null || decidedBy.isBlank()) {
            throw DecisionIncompleteException.decidedByMissing();
        }
        if (!COMPLETED.equals(check.status())) {
            throw new CheckNotCompletedException(check.checkId());
        }
        if (check.decided()) {
            throw new DecisionAlreadyRecordedException(check.checkId());
        }
    }
}
