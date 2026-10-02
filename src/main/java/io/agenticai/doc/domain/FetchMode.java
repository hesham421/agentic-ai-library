package io.agenticai.doc.domain;

import java.util.Optional;

/**
 * How a Check obtains a service's documents — DOC's own copy of the closed fetch mode
 * (CON-DOC-002; ADR-DOC-010). The stored values are exactly {@code path | blob | manual}, the
 * values a consumer stores as {@code VARCHAR2(10 CHAR)}; no fourth fetch mode exists. Every
 * Document Outcome carries its source mode from this list (REQ-DOC-001, REQ-DOC-038).
 *
 * <p>DOC does not import REG's enum of the same values: each module owns its closed lookups in
 * its own code (ADR-DOC-010) and reaches REG only through REG's published contract.
 */
public enum FetchMode {

    PATH("path"),
    BLOB("blob"),
    MANUAL("manual");

    private final String storedValue;

    FetchMode(String storedValue) {
        this.storedValue = storedValue;
    }

    /** The value as a consumer stores it ({@code path}, {@code blob} or {@code manual}). */
    public String storedValue() {
        return storedValue;
    }

    /**
     * The fetch mode of a stored value; empty when the value is not one of the closed set.
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
