package io.agenticai.rpt.contract;

import java.util.List;

/**
 * The in-process interface of the Report Store (RPT) that Host Integration is given
 * (contract-rpt.md; profile {@code module_interface: in_process}). This package is the only RPT
 * package another module may import. Every value object is an immutable record with unmodifiable
 * lists; the closed lookups travel as their codes. No entity crosses this boundary.
 *
 * <p>Refusals are the typed exceptions of this package ({@link RptRefusalException} subclasses,
 * codes of {@link ReportRejectionCodes}); the two read validations raise the module's catalog
 * codes (RPT-400-REQUEST-KEYS-MISSING, RPT-400-SERVICE-CODE-MISSING). RPT never calls an Approval
 * API and never sets a decision on its own (REQ-RPT-037). No caller authentication in this version
 * (raw-idea A2).
 */
public interface ReportStore {

    /**
     * CON-RPT-003 — reads a Check and its report; the same service method serves API-RPT-001.
     *
     * @throws CheckNotFoundException REQ-RPT-025 — no such Check (never created or purged)
     */
    CheckReport readCheck(Long checkId);

    /**
     * CON-RPT-004 — the newest 100 Checks of a service code and request number, with the total;
     * the same service method serves API-RPT-002. Both values are matched exactly, as bound
     * parameters (REQ-RPT-050). A value absent or blank is refused with
     * {@code RPT-400-REQUEST-KEYS-MISSING} (RULE-RPT-009).
     */
    ChecksOfRequest listChecksOfRequest(String serviceCode, String requestNumber);

    /**
     * CON-RPT-005 — the decision agreement of a service; the same service method serves
     * API-RPT-003. A service code absent or blank is refused with
     * {@code RPT-400-SERVICE-CODE-MISSING} (RULE-RPT-015).
     *
     * @return unmodifiable, ordered by version number descending, Overall Status, decision; empty
     *         when nothing is decided
     */
    List<AgreementRow> readDecisionAgreement(String serviceCode);

    /**
     * CON-RPT-006 — records the Employee Decision on a COMPLETED Check, once, in the caller's
     * transaction (REQUIRED). {@code decidedBy} is kept exactly as sent; {@code decidedAt} is RPT's
     * recording time (ADR-RPT-009). The Check's report never changes (REQ-RPT-020).
     *
     * @param employeeDecision    the EMPLOYEE_DECISION code, APPROVED or REJECTED (CON-RPT-002)
     * @param approvalApiExecuted whether INT executed it through the Approval API; always given
     * @throws DecisionIncompleteException       RULE-RPT-013
     * @throws ApprovalFlagOnRejectionException  RULE-RPT-014
     * @throws CheckNotFoundException            REQ-RPT-038
     * @throws DecisionAlreadyRecordedException  RULE-RPT-011
     * @throws CheckNotCompletedException        RULE-RPT-012
     */
    RecordedDecision recordDecision(Long checkId,
                                    String employeeDecision,
                                    String decidedBy,
                                    Boolean approvalApiExecuted);
}
