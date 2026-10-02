package io.agenticai.chk.domain;

import java.util.Optional;

/**
 * The closed Overall Status of a COMPLETED Check (CON-CHK-001; ADR-CHK-001, ADR-CHK-002): the
 * profile's list, mastered and carried by value by CHK through the Check result port. The stored
 * values are the constant names, which a consumer stores as {@code VARCHAR2(30 CHAR)}; RPT keeps
 * them with no runtime read of CHK. Adding a value is a new CHK version.
 *
 * <p>{@link #NOT_COMPLIANT} when any finding is NOT_SATISFIED, otherwise
 * {@link #NEEDS_MANUAL_REVIEW} when any finding is UNDETERMINED or any service query was not read,
 * otherwise {@link #COMPLIANT} (ADR-CHK-002). A FAILED Check never carries one (REQ-CHK-054).
 */
public enum OverallStatus {

    COMPLIANT,
    NOT_COMPLIANT,
    NEEDS_MANUAL_REVIEW;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<OverallStatus> fromStored(String storedValue) {
        for (OverallStatus status : values()) {
            if (status.storedValue().equals(storedValue)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
