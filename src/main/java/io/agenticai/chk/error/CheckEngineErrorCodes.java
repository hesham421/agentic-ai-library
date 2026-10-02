package io.agenticai.chk.error;

/**
 * The error codes of the Check Engine (CHK): exactly the rows of the module's
 * {@code error-catalog} block, in the platform format {@code {MOD}-{http}[-{SLUG}]}
 * (ADR-CHK-018) — all three PLATFORM-STD rows of API-CHK-001.
 *
 * <p>The in-process rejection codes of the start and upload-confirmation operations
 * ({@code CHK-400-START-INCOMPLETE}, {@code CHK-422-SERVICE-NOT-AVAILABLE},
 * {@code CHK-422-CONNECTION-NOT-ACTIVATED}, {@code CHK-404-CHECK-NOT-FOUND},
 * {@code CHK-409-CHECK-NOT-AWAITING-DOCUMENTS}) are not catalog rows — they are typed exceptions of
 * the in-process interface, owned by the SVC-API phase, that INT maps to its own ProblemDetail
 * (ADR-CHK-018) — and are not listed here.
 */
public final class CheckEngineErrorCodes {

    private CheckEngineErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** PLATFORM-STD — the {@code checkId} path parameter is not a number (API-CHK-001). */
    public static final String CHECK_ID_INVALID = "CHK-400-CHECK-ID-INVALID";

    /**
     * PLATFORM-STD — no Active Check exists for the {@code checkId}: the Check has ended or does
     * not exist (API-CHK-001). Message argument: {@code {checkId}}.
     */
    public static final String ACTIVE_CHECK_NOT_FOUND = "CHK-404-ACTIVE-CHECK-NOT-FOUND";

    /** PLATFORM-STD — an unexpected server failure (API-CHK-001). */
    public static final String UNEXPECTED_FAILURE = "CHK-500";
}
