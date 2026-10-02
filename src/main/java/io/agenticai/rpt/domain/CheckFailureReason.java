package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * The closed reason a Check ended FAILED, as RPT stores it (CON-CHK-003; column
 * {@code RPT_CHECK_RUN.FAILURE_REASON}, CHECK constraint {@code CHK_RPT_CHECK_RUN_FAILURE_REASON}).
 * The stored values are the constant names; only a FAILED Check carries one.
 *
 * <p>RPT-side copy: RPT does not import the owning module's {@code domain} package (module boundary,
 * M.1); the value arrives as a code through the Check result port and is matched with
 * {@link #fromStored(String)}. Mapped by JPA through {@code CheckFailureReasonConverter}, which writes {@link #storedValue()} — the same values
 * {@code EnumType.STRING} would write (RPT CORE R1), but one mechanism for every RPT closed list
 * (DATA-DOM).
 */
public enum CheckFailureReason {

    TIMED_OUT,
    MODEL_UNAVAILABLE,
    MODEL_OUTPUT_INVALID,
    MODEL_NOT_PERMITTED,
    UPLOAD_WINDOW_EXPIRED,
    INTERRUPTED,
    INTERNAL_ERROR;

    /** The value as stored — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<CheckFailureReason> fromStored(String storedValue) {
        for (CheckFailureReason value : values()) {
            if (value.storedValue().equals(storedValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
