package io.agenticai.doc.error;

/**
 * The error codes of Document Access (DOC): exactly the rows of the module's
 * {@code error-catalog} block, in the platform format {@code {MOD}-{http}[-{SLUG}]}
 * (ADR-DOC-012).
 *
 * <p>The in-process rejection codes of the handover and fetch operations
 * ({@code DOC-422-FETCH-MODE-NOT-MANUAL}, {@code DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE},
 * {@code DOC-400-INCOMPLETE-UPLOAD}, {@code DOC-409-CHECK-ENDED},
 * {@code DOC-422-UPLOAD-LIMIT-REACHED}, {@code DOC-404-SERVICE-VERSION-NOT-FOUND}) are not
 * catalog rows — they are typed exceptions of the in-process interface that INT maps to its own
 * ProblemDetail (ADR-DOC-012) — and are not listed here.
 */
public final class DocumentAccessErrorCodes {

    private DocumentAccessErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** PLATFORM-STD — the {@code checkId} query parameter is missing or not a number (API-DOC-001). */
    public static final String CHECK_ID_REQUIRED = "DOC-400-CHECK-ID-REQUIRED";

    /** PLATFORM-STD — an unexpected server failure (API-DOC-001). */
    public static final String UNEXPECTED_FAILURE = "DOC-500";
}
