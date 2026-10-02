package io.agenticai.integration.error;

/**
 * The error codes of Host Integration (INT): exactly INT's OWN rows of the module's
 * {@code error-catalog} block, in the platform format {@code {MOD}-{http}[-{SLUG}]} (ADR-INT-003,
 * ADR-INT-017).
 *
 * <p>The catalog's PASS-THROUGH rows ({@code CHK-…}, {@code DOC-…}, {@code RPT-…}) and the Report
 * Store codes INT raises under RULE-INT-002 / RULE-INT-003 (ADR-INT-010) belong to their owners:
 * they are raised and carried by the owners' {@code .contract} exceptions and are not listed here.
 */
public final class IntegrationErrorCodes {

    private IntegrationErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** PLATFORM-STD — the body, a multipart part or the checkId cannot be read (REQ-INT-007). */
    public static final String REQUEST_INVALID = "INT-400-REQUEST-INVALID";

    /** RULE-INT-001 — an upload for a Check whose status is not AWAITING_DOCUMENTS (REQ-INT-011). */
    public static final String CHECK_NOT_AWAITING_DOCUMENTS = "INT-409-CHECK-NOT-AWAITING-DOCUMENTS";

    /** PLATFORM-STD — the upload request exceeds the upload request limit (REQ-INT-014). */
    public static final String UPLOAD_TOO_LARGE = "INT-413-UPLOAD-TOO-LARGE";

    /** PLATFORM-STD — the host Approval API answers outside 2xx, cannot be reached or has no address (REQ-INT-036). */
    public static final String APPROVAL_API_FAILED = "INT-502-APPROVAL-API-FAILED";

    /** PLATFORM-STD — the host Approval API does not answer within the approval timeout (REQ-INT-037). */
    public static final String APPROVAL_API_TIMED_OUT = "INT-504-APPROVAL-API-TIMED-OUT";

    /** PLATFORM-STD — an unexpected server failure (REQ-INT-008). */
    public static final String UNEXPECTED_FAILURE = "INT-500";
}
