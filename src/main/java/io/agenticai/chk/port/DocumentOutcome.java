package io.agenticai.chk.port;

import java.util.Objects;

/**
 * CHK's immutable copy of the outcome of one document of a Check (REQ-CHK-017): exactly one read
 * status, a reason and a detail when UNREADABLE, the content when READ. CHK services work on this
 * type only, never on Document Access's contract types.
 *
 * <p>The closed lookups travel as Document Access's codes, the values RPT stores through the
 * Check result port: {@code readStatus} is {@link #READ}, {@link #MISSING} or {@link #UNREADABLE}
 * and {@code reason} one of the UNREADABLE reasons (CON-DOC-001); {@code sourceMode} is
 * {@code path}, {@code blob} or {@code manual} (CON-DOC-002). Whether an outcome blocks
 * COMPLIANT is CHK's decision; nothing that could not be read is dropped (guardrail G6).
 *
 * @param documentType the document type; {@code null} only for a host document whose type column
 *                     was NULL
 * @param sourceMode   the fetch mode of the Check's service package version
 * @param readStatus   READ, MISSING or UNREADABLE
 * @param reason       the reason code, UNREADABLE only; {@code null} otherwise
 * @param detail       the text naming the failure, UNREADABLE only; {@code null} otherwise — never
 *                     document content
 * @param content      the content, READ only; {@code null} otherwise
 */
public record DocumentOutcome(String documentType,
                              String sourceMode,
                              String readStatus,
                              String reason,
                              String detail,
                              DocumentContent content) {

    /** CON-DOC-001 — the document was fetched and read. */
    public static final String READ = "READ";

    /** CON-DOC-001 — a required document type no document carries. */
    public static final String MISSING = "MISSING";

    /** CON-DOC-001 — the document was listed or uploaded but could not be fetched or read. */
    public static final String UNREADABLE = "UNREADABLE";

    public DocumentOutcome {
        Objects.requireNonNull(sourceMode, "sourceMode");
        Objects.requireNonNull(readStatus, "readStatus");
        switch (readStatus) {
            case READ -> Objects.requireNonNull(content, "content");
            case MISSING -> Objects.requireNonNull(documentType, "documentType");
            case UNREADABLE -> {
                Objects.requireNonNull(reason, "reason");
                Objects.requireNonNull(detail, "detail");
            }
            default -> throw new IllegalArgumentException(
                    "readStatus must be READ, MISSING or UNREADABLE: " + readStatus);
        }
        if (!READ.equals(readStatus) && content != null) {
            throw new IllegalArgumentException("only a READ outcome carries content");
        }
        if (!UNREADABLE.equals(readStatus) && (reason != null || detail != null)) {
            throw new IllegalArgumentException("only an UNREADABLE outcome carries a reason and a detail");
        }
    }

    /** Whether the document was read. */
    public boolean isRead() {
        return READ.equals(readStatus);
    }
}
