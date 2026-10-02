package io.agenticai.rpt.contract;

/**
 * The in-process rejection codes of the Report Store — the sixteen rows of the SVC-API in-process
 * rejection table (ADR-RPT-012, ADR-RPT-013 point 2): typed exceptions of the Check result port
 * implementation and of {@link ReportStore}, each carrying a code in the profile format
 * {@code RPT-{http}[-{SLUG}]} and the RULE / REQ message. They are NOT rows of the module's error
 * catalog (that holds only the five rows kept in {@code ReportStoreErrorCodes}): INT maps the
 * decision codes to its own ProblemDetail. Their English texts are in {@code messages.properties},
 * keyed by code (a code with several messages: by code and a {@code .SLUG} suffix).
 *
 * <p>{@link #CHECK_NOT_FOUND} carries the same code and text as the catalog row of API-RPT-001
 * (REQ-RPT-007, REQ-RPT-025, REQ-RPT-038): one code, raised in-process and over HTTP alike.
 */
public final class ReportRejectionCodes {

    private ReportRejectionCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** RULE-RPT-001 — a Check run value absent or blank ({@link CheckRunIncompleteException}). */
    public static final String CHECK_RUN_INCOMPLETE = "RPT-400-CHECK-RUN-INCOMPLETE";

    /** RULE-RPT-002 — the initial status disagrees with the fetch mode ({@link InitialStatusMismatchException}). */
    public static final String INITIAL_STATUS_MISMATCH = "RPT-422-INITIAL-STATUS-MISMATCH";

    /** RULE-RPT-003 — the Check has already ended ({@link CheckEndedException}). */
    public static final String CHECK_ENDED = "RPT-409-CHECK-ENDED";

    /** RULE-RPT-003 — the Check is not running and cannot be completed ({@link CheckNotRunningException}). */
    public static final String CHECK_NOT_RUNNING = "RPT-409-CHECK-NOT-RUNNING";

    /** REQ-RPT-007, REQ-RPT-038 — no Check Run for the identifier ({@link CheckNotFoundException}). */
    public static final String CHECK_NOT_FOUND = "RPT-404-CHECK-NOT-FOUND";

    /** RULE-RPT-004 — the report metadata is missing or differs from the Check run ({@link MetadataMismatchException}). */
    public static final String METADATA_MISMATCH = "RPT-422-METADATA-MISMATCH";

    /** RULE-RPT-005 — COMPLIANT without every finding SATISFIED and every query read ({@link CompliantNotVerifiedException}). */
    public static final String COMPLIANT_NOT_VERIFIED = "RPT-422-COMPLIANT-NOT-VERIFIED";

    /** RULE-RPT-006 — a value outside its closed list ({@link UnknownCodeException}). */
    public static final String UNKNOWN_CODE = "RPT-422-UNKNOWN-CODE";

    /** RULE-RPT-007 — a finding is incomplete ({@link FindingIncompleteException}). */
    public static final String FINDING_INCOMPLETE = "RPT-422-FINDING-INCOMPLETE";

    /** RULE-RPT-008 — a reason not exactly on UNREADABLE ({@link DocumentReasonMismatchException}). */
    public static final String DOCUMENT_REASON_MISMATCH = "RPT-422-DOCUMENT-REASON-MISMATCH";

    /** RULE-RPT-010 — a failure is incomplete ({@link FailureIncompleteException}). */
    public static final String FAILURE_INCOMPLETE = "RPT-400-FAILURE-INCOMPLETE";

    /** RULE-RPT-011 — the Check already has an Employee Decision ({@link DecisionAlreadyRecordedException}). */
    public static final String DECISION_ALREADY_RECORDED = "RPT-409-DECISION-ALREADY-RECORDED";

    /** RULE-RPT-012 — a decision on a Check that is not COMPLETED ({@link CheckNotCompletedException}). */
    public static final String CHECK_NOT_COMPLETED = "RPT-409-CHECK-NOT-COMPLETED";

    /** RULE-RPT-013 — a decision is incomplete ({@link DecisionIncompleteException}). */
    public static final String DECISION_INCOMPLETE = "RPT-400-DECISION-INCOMPLETE";

    /** RULE-RPT-014 — Approval API execution claimed on a REJECTED decision ({@link ApprovalFlagOnRejectionException}). */
    public static final String APPROVAL_FLAG_ON_REJECTION = "RPT-422-APPROVAL-FLAG-ON-REJECTION";

    /** REQ-RPT-009 — a database failure while the report was written ({@link ReportNotStoredException}). */
    public static final String REPORT_NOT_STORED = "RPT-500-REPORT-NOT-STORED";
}
