package io.agenticai.doc.domain;

import java.util.Optional;

/**
 * The closed reason of an UNREADABLE Document Outcome (CON-DOC-001; ADR-DOC-010). The stored
 * values are the constant names, which a consumer stores as {@code VARCHAR2(30 CHAR)}. Adding a
 * value is a new DOC version.
 *
 * <ul>
 *   <li>{@link #OUTSIDE_STORAGE_ROOT} — a {@code path} document outside the configured storage
 *       root, or no storage root set (REQ-DOC-010, REQ-DOC-011)</li>
 *   <li>{@link #NOT_FOUND} — the listed file or row does not exist</li>
 *   <li>{@link #TOO_LARGE} — larger than {@code aias.check.max-file-size} (REQ-DOC-042)</li>
 *   <li>{@link #UNSUPPORTED_FORMAT} — a format no reader handles</li>
 *   <li>{@link #READING_FAILED} — the reading step failed, including no document-reading model
 *       configured (REQ-DOC-033)</li>
 *   <li>{@link #OUT_OF_TIME} — the Check's timeout was reached first (REQ-DOC-040)</li>
 *   <li>{@link #SOURCE_QUERY_FAILED} — the document source query failed or exceeded
 *       {@code aias.check.max-rows} (REQ-DOC-039)</li>
 *   <li>{@link #MODEL_NOT_PERMITTED} — a FREE-tier model with REAL data (REQ-DOC-058,
 *       ADR-DOC-009)</li>
 * </ul>
 */
public enum UnreadableReason {

    OUTSIDE_STORAGE_ROOT,
    NOT_FOUND,
    TOO_LARGE,
    UNSUPPORTED_FORMAT,
    READING_FAILED,
    OUT_OF_TIME,
    SOURCE_QUERY_FAILED,
    MODEL_NOT_PERMITTED;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The reason of a stored value; empty when the value is not one of the closed set.
     */
    public static Optional<UnreadableReason> fromStored(String storedValue) {
        for (UnreadableReason reason : values()) {
            if (reason.storedValue().equals(storedValue)) {
                return Optional.of(reason);
            }
        }
        return Optional.empty();
    }
}
