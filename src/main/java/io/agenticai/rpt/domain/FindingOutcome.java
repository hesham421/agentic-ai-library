package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * The closed outcome of one finding as RPT stores it (CON-CHK-002; column
 * {@code RPT_FINDING.FINDING_OUTCOME}, CHECK constraint {@code CHK_RPT_FINDING_FINDING_OUTCOME}).
 * The stored values are the constant names.
 *
 * <p>RPT-side copy: RPT does not import the owning module's {@code domain} package (module boundary,
 * M.1); the value arrives as a code through the Check result port and is matched with
 * {@link #fromStored(String)}. Mapped by JPA through {@code FindingOutcomeConverter}, which writes {@link #storedValue()} — the same values
 * {@code EnumType.STRING} would write (RPT CORE R1), but one mechanism for every RPT closed list
 * (DATA-DOM).
 */
public enum FindingOutcome {

    SATISFIED,
    NOT_SATISFIED,
    UNDETERMINED;

    /** The value as stored — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<FindingOutcome> fromStored(String storedValue) {
        for (FindingOutcome value : values()) {
            if (value.storedValue().equals(storedValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
