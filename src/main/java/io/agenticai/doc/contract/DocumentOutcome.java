package io.agenticai.doc.contract;

import java.util.Objects;

/**
 * CON-DOC-004 — the outcome of one document of a Check: exactly one read status, with a reason
 * and a detail when UNREADABLE and the content when READ (REQ-DOC-034, REQ-DOC-036,
 * REQ-DOC-038). Immutable; transient — DOC stores none of it (ADR-DOC-002, ADR-DOC-008).
 *
 * <p>The closed lookups travel as their codes, the values a consumer stores (RPT, through CHK,
 * with no runtime read of DOC): {@code readStatus} is one of {@link #READ}, {@link #MISSING},
 * {@link #UNREADABLE} (CON-DOC-001); {@code reason} is one of {@code OUTSIDE_STORAGE_ROOT,
 * NOT_FOUND, TOO_LARGE, UNSUPPORTED_FORMAT, READING_FAILED, OUT_OF_TIME, SOURCE_QUERY_FAILED,
 * MODEL_NOT_PERMITTED} (CON-DOC-001); {@code sourceMode} is {@code path}, {@code blob} or
 * {@code manual} (CON-DOC-002). Built through {@link #read}, {@link #missing} and
 * {@link #unreadable} only.
 *
 * @param documentType the document type the document carries — the host's type column
 *                     (REQ-DOC-006) or the employee's (REQ-DOC-017); for a MISSING outcome the
 *                     required type no document carried (REQ-DOC-035). {@code null} only for a
 *                     host row whose type column was NULL: such a document is reported, never
 *                     dropped, and satisfies no required type
 * @param sourceMode   the fetch mode of the Check's service package version (CON-DOC-002)
 * @param readStatus   {@link #READ}, {@link #MISSING} or {@link #UNREADABLE} (CON-DOC-001)
 * @param reason       the reason code, UNREADABLE only; {@code null} otherwise
 * @param detail       the text naming the failure, UNREADABLE only; {@code null} otherwise.
 *                     Names a location, a size or a failure — never document content
 * @param content      the content, READ only; {@code null} otherwise
 */
public record DocumentOutcome(String documentType,
                              String sourceMode,
                              String readStatus,
                              String reason,
                              String detail,
                              DocumentContent content) {

    /** CON-DOC-001 — the document was fetched and read; {@link #content()} is present. */
    public static final String READ = "READ";

    /** CON-DOC-001 — a required document type that no fetched or uploaded document carries. */
    public static final String MISSING = "MISSING";

    /** CON-DOC-001 — the document was listed or uploaded but could not be fetched or read. */
    public static final String UNREADABLE = "UNREADABLE";

    public DocumentOutcome {
        Objects.requireNonNull(sourceMode, "sourceMode");
        Objects.requireNonNull(readStatus, "readStatus");
        switch (readStatus) {
            case READ -> {
                Objects.requireNonNull(content, "content");
                requireNone(reason, detail, "a READ outcome carries no reason and no detail");
            }
            case MISSING -> {
                Objects.requireNonNull(documentType, "documentType");
                requireNone(reason, detail, "a MISSING outcome carries no reason and no detail");
                requireNoContent(content, "a MISSING outcome carries no content");
            }
            case UNREADABLE -> {
                Objects.requireNonNull(reason, "reason");
                Objects.requireNonNull(detail, "detail");
                requireNoContent(content, "an UNREADABLE outcome carries no content");
            }
            default -> throw new IllegalArgumentException(
                    "readStatus must be READ, MISSING or UNREADABLE: " + readStatus);
        }
    }

    /** A READ outcome with its content (REQ-DOC-038). */
    public static DocumentOutcome read(String documentType, String sourceMode, DocumentContent content) {
        return new DocumentOutcome(documentType, sourceMode, READ, null, null, content);
    }

    /** A MISSING outcome for a required document type no document carries (REQ-DOC-035). */
    public static DocumentOutcome missing(String documentType, String sourceMode) {
        return new DocumentOutcome(documentType, sourceMode, MISSING, null, null, null);
    }

    /** An UNREADABLE outcome with its one reason code and its detail (REQ-DOC-036). */
    public static DocumentOutcome unreadable(String documentType, String sourceMode, String reason, String detail) {
        return new DocumentOutcome(documentType, sourceMode, UNREADABLE, reason, detail, null);
    }

    private static void requireNone(String reason, String detail, String message) {
        if (reason != null || detail != null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNoContent(DocumentContent content, String message) {
        if (content != null) {
            throw new IllegalArgumentException(message);
        }
    }
}
