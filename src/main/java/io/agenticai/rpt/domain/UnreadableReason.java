package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * The closed reason a Check Document was UNREADABLE, as RPT stores it (CON-DOC-001; column
 * {@code RPT_CHECK_DOCUMENT.UNREADABLE_REASON}, CHECK constraint
 * {@code CHK_RPT_CHECK_DOCUMENT_UNREADABLE_REASON}). The stored values are the constant names;
 * only an UNREADABLE document carries one.
 *
 * <p>RPT-side copy: RPT does not import the owning module's {@code domain} package (module boundary,
 * M.1); the value arrives as a code through the Check result port and is matched with
 * {@link #fromStored(String)}. Mapped by JPA through {@code UnreadableReasonConverter}, which writes {@link #storedValue()} — the same values
 * {@code EnumType.STRING} would write (RPT CORE R1), but one mechanism for every RPT closed list
 * (DATA-DOM).
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

    /** The value as stored — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<UnreadableReason> fromStored(String storedValue) {
        for (UnreadableReason value : values()) {
            if (value.storedValue().equals(storedValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
