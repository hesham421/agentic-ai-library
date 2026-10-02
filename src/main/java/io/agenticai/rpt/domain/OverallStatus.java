package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * The Overall Status of a COMPLETED Check as RPT stores it (CON-CHK-001; column
 * {@code RPT_CHECK_RUN.OVERALL_STATUS}, CHECK constraint {@code CHK_RPT_CHECK_RUN_OVERALL_STATUS}).
 * The stored values are the constant names; a Check that is not COMPLETED carries none.
 *
 * <p>RPT-side copy: RPT does not import the owning module's {@code domain} package (module boundary,
 * M.1); the value arrives as a code through the Check result port and is matched with
 * {@link #fromStored(String)}. Mapped by JPA through {@code OverallStatusConverter}, which writes {@link #storedValue()} — the same values
 * {@code EnumType.STRING} would write (RPT CORE R1), but one mechanism for every RPT closed list
 * (DATA-DOM).
 */
public enum OverallStatus {

    COMPLIANT,
    NOT_COMPLIANT,
    NEEDS_MANUAL_REVIEW;

    /** The value as stored — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<OverallStatus> fromStored(String storedValue) {
        for (OverallStatus value : values()) {
            if (value.storedValue().equals(storedValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
