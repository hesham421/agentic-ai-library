package io.agenticai.reg.domain;

import java.util.Optional;

/**
 * How a Check obtains a service's documents (ENT-REG-002.fetchMode). Closed: the stored values
 * are exactly those of {@code CHK_REG_SVC_PKG_VER_FETCH_MODE} — {@code path | blob | manual}.
 */
public enum FetchMode {

    PATH("path"),
    BLOB("blob"),
    MANUAL("manual");

    private final String storedValue;

    FetchMode(String storedValue) {
        this.storedValue = storedValue;
    }

    /** The value as stored in {@code FETCH_MODE}. */
    public String storedValue() {
        return storedValue;
    }

    /**
     * The fetch mode of a stored value; empty when the value is not one of the closed set
     * (an unknown fetch mode is a REJECTED load reason, RULE-REG-008 — never an HTTP error).
     */
    public static Optional<FetchMode> fromStored(String storedValue) {
        for (FetchMode mode : values()) {
            if (mode.storedValue.equals(storedValue)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
