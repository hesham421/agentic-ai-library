package io.agenticai.rpt.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * The Employee Decision handed in with {@code recordDecision}, and the decisions whether it may be
 * recorded (DATA-DOM RULE-RPT-011 … RULE-RPT-014, owner layer domain). The decision is not a
 * status: it is written beside a COMPLETED result, at most once.
 *
 * <ul>
 *   <li>{@link #refusal()} — on the values received, before any write, in the SVC-API order:
 *       RULE-RPT-013 (a code of EMPLOYEE_DECISION, a deciding employee not blank, a yes / no
 *       for the Approval API) then RULE-RPT-014 (never REJECTED executed through the Approval
 *       API).</li>
 *   <li>{@link #refusalOn(CheckStatus, EmployeeDecision)} — on the stored run: RULE-RPT-011 (no
 *       decision yet) then RULE-RPT-012 (COMPLETED). SVC-API applies it to diagnose a conditional
 *       UPDATE ({@code WHERE CHECK_STATUS = 'COMPLETED' AND EMPLOYEE_DECISION IS NULL}, the
 *       repository guard) that changed 0 rows.</li>
 * </ul>
 *
 * The decision code is taken as the text received so that RULE-RPT-013 can name an unknown value
 * ("`{value}` is not APPROVED or REJECTED"); {@link #decision()} gives the parsed code once
 * accepted. {@code decidedBy} is kept exactly as received (REQ-RPT-002). Plain Java: no
 * framework, no I/O; it returns a decision, the caller raises the SVC-API exception.
 */
public final class DecisionRecording {

    private final String decisionCode;
    private final EmployeeDecision decision;
    private final String decidedBy;
    private final Boolean approvalApiExecuted;

    private DecisionRecording(String decisionCode, String decidedBy, Boolean approvalApiExecuted) {
        this.decisionCode = decisionCode;
        this.decision = decisionCode == null ? null : EmployeeDecision.fromStored(decisionCode).orElse(null);
        this.decidedBy = decidedBy;
        this.approvalApiExecuted = approvalApiExecuted;
    }

    /** The decision as received; any value may be {@code null} — {@link #refusal()} decides. */
    public static DecisionRecording create(String decisionCode, String decidedBy, Boolean approvalApiExecuted) {
        return new DecisionRecording(decisionCode, decidedBy, approvalApiExecuted);
    }

    /** The first rule the received values break (RULE-RPT-013, then RULE-RPT-014), or empty. */
    public Optional<RuleRefusal> refusal() {
        if (decision == null) {
            return Optional.of(RuleRefusal.of(RefusalReason.DECISION_CODE_UNKNOWN, decisionCode));
        }
        if (Texts.isBlank(decidedBy)) {
            return Optional.of(RuleRefusal.of(RefusalReason.DECIDED_BY_MISSING));
        }
        if (approvalApiExecuted == null) {
            return Optional.of(RuleRefusal.of(RefusalReason.APPROVAL_FLAG_MISSING));
        }
        if (decision == EmployeeDecision.REJECTED && approvalApiExecuted) {
            return Optional.of(RuleRefusal.of(RefusalReason.APPROVAL_FLAG_ON_REJECTION));
        }
        return Optional.empty();
    }

    /**
     * Whether the stored run may take a decision: RULE-RPT-011 (none recorded yet), then
     * RULE-RPT-012 (status COMPLETED).
     *
     * @param status         the stored Check status
     * @param storedDecision the stored Employee Decision, or {@code null}
     */
    public static Optional<RuleRefusal> refusalOn(CheckStatus status, EmployeeDecision storedDecision) {
        Objects.requireNonNull(status, "status");
        if (storedDecision != null) {
            return Optional.of(RuleRefusal.of(RefusalReason.DECISION_ALREADY_RECORDED));
        }
        if (status != CheckStatus.COMPLETED) {
            return Optional.of(RuleRefusal.of(RefusalReason.CHECK_NOT_COMPLETED));
        }
        return Optional.empty();
    }

    /** The parsed decision; {@code null} when the code received is not one of EMPLOYEE_DECISION. */
    public EmployeeDecision decision() {
        return decision;
    }

    public String decidedBy() {
        return decidedBy;
    }

    public Boolean approvalApiExecuted() {
        return approvalApiExecuted;
    }
}
