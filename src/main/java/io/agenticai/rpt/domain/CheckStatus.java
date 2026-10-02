package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * The closed status of a Check as RPT stores it (CON-CHK-001; column
 * {@code RPT_CHECK_RUN.CHECK_STATUS}, CHECK constraint {@code CHK_RPT_CHECK_RUN_CHECK_STATUS}).
 * The stored values are the constant names. COMPLETED and FAILED are final.
 *
 * <p>RPT-side copy: RPT does not import the owning module's {@code domain} package (module boundary,
 * M.1); the value arrives as a code through the Check result port and is matched with
 * {@link #fromStored(String)}. Mapped by JPA through {@code CheckStatusConverter}, which writes {@link #storedValue()} — the same values
 * {@code EnumType.STRING} would write (RPT CORE R1), but one mechanism for every RPT closed list
 * (DATA-DOM).
 */
public enum CheckStatus {

    AWAITING_DOCUMENTS,
    RUNNING,
    COMPLETED,
    FAILED;

    /** The value as stored — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<CheckStatus> fromStored(String storedValue) {
        for (CheckStatus value : values()) {
            if (value.storedValue().equals(storedValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
