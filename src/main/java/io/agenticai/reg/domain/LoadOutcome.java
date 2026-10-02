package io.agenticai.reg.domain;

import java.util.Optional;

/**
 * The outcome of one subject of a load run (ENT-REG-006.outcome). Closed: the stored values are
 * exactly those of {@code CHK_REG_LOAD_RESULT_OUTCOME}.
 */
public enum LoadOutcome {

    REGISTERED("REGISTERED"),
    UNCHANGED("UNCHANGED"),
    REJECTED("REJECTED"),
    WITHDRAWN("WITHDRAWN"),
    ACTIVATED("ACTIVATED"),
    UPDATED("UPDATED"),
    REMOVED("REMOVED");

    private final String storedValue;

    LoadOutcome(String storedValue) {
        this.storedValue = storedValue;
    }

    /** The value as stored in {@code OUTCOME}. */
    public String storedValue() {
        return storedValue;
    }

    /** The outcome of a stored value; empty when the value is not one of the closed set. */
    public static Optional<LoadOutcome> fromStored(String storedValue) {
        for (LoadOutcome outcome : values()) {
            if (outcome.storedValue.equals(storedValue)) {
                return Optional.of(outcome);
            }
        }
        return Optional.empty();
    }
}
