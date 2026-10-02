package io.agenticai.doc.contract;

/**
 * The in-process rejection codes of Document Access — the rows of the SVC-API in-process rejection
 * table (ADR-DOC-012): typed exceptions of the {@link DocumentAccess} interface, each carrying a
 * code in the profile format {@code DOC-{http}[-{SLUG}]} and the SRS rule message. They are NOT
 * rows of the module's error catalog (that holds only the two PLATFORM-STD rows of API-DOC-001,
 * kept in {@code DocumentAccessErrorCodes}): INT maps them to its own ProblemDetail.
 *
 * <p>The one catalog code the in-process interface shares, {@code DOC-400-CHECK-ID-REQUIRED}
 * ({@link CheckIdRequiredException}), stays in {@code DocumentAccessErrorCodes} and is not
 * repeated here.
 */
public final class DocumentRejectionCodes {

    private DocumentRejectionCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** RULE-DOC-003 — the upload lacks a Check, a document type or a non-empty file ({@link IncompleteUploadException}). */
    public static final String INCOMPLETE_UPLOAD = "DOC-400-INCOMPLETE-UPLOAD";

    /** REQ-DOC-003 — no such service package version ({@link ServiceVersionNotFoundException}). */
    public static final String SERVICE_VERSION_NOT_FOUND = "DOC-404-SERVICE-VERSION-NOT-FOUND";

    /** RULE-DOC-001 — the version's fetch mode is not {@code manual} ({@link FetchModeNotManualException}). */
    public static final String FETCH_MODE_NOT_MANUAL = "DOC-422-FETCH-MODE-NOT-MANUAL";

    /** RULE-DOC-002 — the document type is not one of the version's required types ({@link DocumentTypeNotOfServiceException}). */
    public static final String DOCUMENT_TYPE_NOT_OF_SERVICE = "DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE";

    /** RULE-DOC-009 — the Check is recorded as ended ({@link CheckEndedException}). */
    public static final String CHECK_ENDED = "DOC-409-CHECK-ENDED";

    /** RULE-DOC-010 — the Check already holds the maximum uploads ({@link UploadLimitReachedException}). */
    public static final String UPLOAD_LIMIT_REACHED = "DOC-422-UPLOAD-LIMIT-REACHED";

    /**
     * RULE-DOC-005 — the notice of an oversized upload, carried in {@link UploadReceipt#notice()}.
     * A message key only ("notice, not an error" in the rejection table): it is never the code of
     * an exception or of a ProblemDetail.
     */
    public static final String OVERSIZED_UPLOAD_NOTICE = "DOC-NOTICE-OVERSIZED-UPLOAD";
}
