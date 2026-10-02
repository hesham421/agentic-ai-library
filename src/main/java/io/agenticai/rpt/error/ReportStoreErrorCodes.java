package io.agenticai.rpt.error;

/**
 * The error codes of the Report Store (RPT): exactly the five rows of the module's
 * {@code error-catalog} block, in the platform format {@code {MOD}-{http}[-{SLUG}]}
 * (ADR-RPT-013).
 *
 * <p>The in-process refusal codes of the Check result port and of the Employee Decision
 * (the SVC-API table, ADR-RPT-013 point 2) are not catalog rows — they are typed exceptions of the
 * in-process interface, owned by the SVC-API phase, that INT maps to its own ProblemDetail — and
 * are not listed here.
 */
public final class ReportStoreErrorCodes {

    private ReportStoreErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** PLATFORM-STD — the {@code checkId} path parameter is not a number (API-RPT-001). */
    public static final String CHECK_ID_INVALID = "RPT-400-CHECK-ID-INVALID";

    /**
     * PLATFORM-STD — no Check Run exists for the {@code checkId}: never created or purged
     * (API-RPT-001, REQ-RPT-025). Message argument: {@code {checkId}}.
     */
    public static final String CHECK_NOT_FOUND = "RPT-404-CHECK-NOT-FOUND";

    /**
     * RULE-RPT-009 — {@code serviceCode} or {@code requestNumber} absent or blank when listing the
     * Checks of a request (API-RPT-002).
     */
    public static final String REQUEST_KEYS_MISSING = "RPT-400-REQUEST-KEYS-MISSING";

    /**
     * RULE-RPT-015 — {@code serviceCode} absent or blank when reading the decision agreement
     * (API-RPT-003).
     */
    public static final String SERVICE_CODE_MISSING = "RPT-400-SERVICE-CODE-MISSING";

    /** PLATFORM-STD — an unexpected server failure (API-RPT-001, API-RPT-002, API-RPT-003). */
    public static final String UNEXPECTED_FAILURE = "RPT-500";
}
