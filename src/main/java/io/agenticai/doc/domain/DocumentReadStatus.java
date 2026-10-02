package io.agenticai.doc.domain;

import java.util.Optional;

/**
 * The closed read status of a document (CON-DOC-001; ADR-DOC-010): every Document Outcome
 * carries exactly one of {@code READ | MISSING | UNREADABLE}. The stored values are the constant
 * names, which a consumer stores as {@code VARCHAR2(30 CHAR)} (RPT's Check Documents, with no
 * runtime read of DOC — ADR-DOC-002, ADR-DOC-007). Adding a value is a new DOC version.
 *
 * <p>An UNREADABLE outcome carries exactly one {@link UnreadableReason} and a detail text; READ
 * and MISSING outcomes carry no reason (REQ-DOC-034, REQ-DOC-035, REQ-DOC-036).
 */
public enum DocumentReadStatus {

    READ,
    MISSING,
    UNREADABLE;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The read status of a stored value; empty when the value is not one of the closed set.
     */
    public static Optional<DocumentReadStatus> fromStored(String storedValue) {
        for (DocumentReadStatus status : values()) {
            if (status.storedValue().equals(storedValue)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
