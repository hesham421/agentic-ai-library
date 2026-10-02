package io.agenticai.rpt.domain;

import java.util.List;

/**
 * Why a domain rule of the Report Store refuses an operation — the decision value the RPT domain
 * classes return. Each constant names the RULE it comes from and one of that RULE's messages
 * (DATA-DOM DOMAIN RULES), and lists the message's placeholders other than {@code {checkId}},
 * which the caller knows.
 *
 * <p>The domain DECIDES and returns this value; it does not throw. The in-process refusal codes
 * ({@code RPT-4xx-…}) and their typed exceptions ({@code RptRefusalException} subclasses) are
 * defined only by the SVC-API phase (its in-process rejection-code table, ADR-RPT-012,
 * ADR-RPT-013), so the service maps each reason to its exception and raises it — no code is
 * invented here.
 */
public enum RefusalReason {

    /** RULE-RPT-001 — "The Check run was not stored: {field} is missing." */
    CHECK_RUN_INCOMPLETE("RULE-RPT-001", "field"),

    /** RULE-RPT-006 — "Not stored: `{value}` is not a code of {lookupKey}." */
    UNKNOWN_CODE("RULE-RPT-006", "value", "lookupKey"),

    /** RULE-RPT-002 — "The Check run was not stored: status AWAITING_DOCUMENTS needs fetch mode manual." */
    AWAITING_NEEDS_MANUAL("RULE-RPT-002"),

    /** RULE-RPT-002 — "The Check run was not stored: a manual Check starts AWAITING_DOCUMENTS." */
    MANUAL_STARTS_AWAITING("RULE-RPT-002"),

    /** RULE-RPT-003 — "Check {checkId} has already ended; its status cannot change." */
    CHECK_ENDED("RULE-RPT-003"),

    /** RULE-RPT-003 — "Check {checkId} is not running; it cannot be completed." */
    CHECK_NOT_RUNNING("RULE-RPT-003"),

    /** RULE-RPT-004 — "The report of Check {checkId} was not stored: its metadata {field} {value} differs from the Check run ({stored})." */
    METADATA_MISMATCH("RULE-RPT-004", "field", "value", "stored"),

    /** RULE-RPT-004 — "The report of Check {checkId} was not stored: {field} is missing." */
    METADATA_MISSING("RULE-RPT-004", "field"),

    /** RULE-RPT-007 — "The report of Check {checkId} was not stored: finding {position} has no {field}." */
    FINDING_INCOMPLETE("RULE-RPT-007", "position", "field"),

    /**
     * RULE-RPT-007 (applied to unread queries, DATA-DOM ENT-RPT-004) — an unread query without a
     * name or a detail. The unit states no message of its own for it (recorded gap); the
     * placeholders are those of {@link #FINDING_INCOMPLETE}.
     */
    UNREAD_QUERY_INCOMPLETE("RULE-RPT-007", "position", "field"),

    /** RULE-RPT-008 — "The report of Check {checkId} was not stored: document {position} is UNREADABLE without a reason." */
    DOCUMENT_REASON_MISSING("RULE-RPT-008", "position"),

    /** RULE-RPT-008 — "The report of Check {checkId} was not stored: document {position} is {readStatus} and cannot carry a reason." */
    DOCUMENT_REASON_NOT_ALLOWED("RULE-RPT-008", "position", "readStatus"),

    /**
     * RULE-RPT-008 — a document outcome without a document type. The rule requires one but states
     * no message for it (recorded gap).
     */
    DOCUMENT_INCOMPLETE("RULE-RPT-008", "position", "field"),

    /** RULE-RPT-005 — "The report of Check {checkId} was not stored: COMPLIANT needs every finding SATISFIED and every service query read." */
    COMPLIANT_NOT_VERIFIED("RULE-RPT-005"),

    /** RULE-RPT-010 — "The failure of Check {checkId} was not stored: {field} is missing." */
    FAILURE_INCOMPLETE("RULE-RPT-010", "field"),

    /** RULE-RPT-011 — "Check {checkId} already has an Employee Decision." */
    DECISION_ALREADY_RECORDED("RULE-RPT-011"),

    /** RULE-RPT-012 — "Check {checkId} is not completed; a decision can only be recorded on a completed Check." */
    CHECK_NOT_COMPLETED("RULE-RPT-012"),

    /** RULE-RPT-013 — "The decision was not recorded: `{value}` is not APPROVED or REJECTED." */
    DECISION_CODE_UNKNOWN("RULE-RPT-013", "value"),

    /** RULE-RPT-013 — "The decision was not recorded: the deciding employee is missing." */
    DECIDED_BY_MISSING("RULE-RPT-013"),

    /** RULE-RPT-013 — "The decision was not recorded: say whether it was executed through the Approval API." */
    APPROVAL_FLAG_MISSING("RULE-RPT-013"),

    /** RULE-RPT-014 — "The decision was not recorded: only an APPROVED decision is executed through the Approval API." */
    APPROVAL_FLAG_ON_REJECTION("RULE-RPT-014");

    private final String rule;
    private final String[] placeholders;

    RefusalReason(String rule, String... placeholders) {
        this.rule = rule;
        this.placeholders = placeholders;
    }

    /** The RULE id the refusal comes from, e.g. {@code RULE-RPT-003}. */
    public String rule() {
        return rule;
    }

    /** The message placeholders the refusal carries, in order of appearance ({@code {checkId}} excluded). */
    public List<String> placeholders() {
        return List.of(placeholders);
    }
}
