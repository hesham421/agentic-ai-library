package io.agenticai.chk.domain;

import java.util.Optional;

/**
 * The closed outcome of one finding (CON-CHK-002; ADR-CHK-001, ADR-CHK-002). The stored values are
 * the constant names, which a consumer stores as {@code VARCHAR2(30 CHAR)}. A finding whose
 * evidence is not in the Check's own data is {@link #UNDETERMINED} (REQ-CHK-029, REQ-CHK-040).
 * Adding a value is a new CHK version.
 */
public enum FindingOutcome {

    SATISFIED,
    NOT_SATISFIED,
    UNDETERMINED;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<FindingOutcome> fromStored(String storedValue) {
        for (FindingOutcome outcome : values()) {
            if (outcome.storedValue().equals(storedValue)) {
                return Optional.of(outcome);
            }
        }
        return Optional.empty();
    }
}
