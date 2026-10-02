package io.agenticai.chk.domain;

import java.util.Optional;

/**
 * The closed status of a Check (CON-CHK-001; ADR-CHK-001, ADR-CHK-016). The stored values are the
 * constant names, which a consumer stores as {@code VARCHAR2(30 CHAR)}. {@link #COMPLETED} and
 * {@link #FAILED} are final; a {@code manual} Check starts {@link #AWAITING_DOCUMENTS} and a
 * {@code path} / {@code blob} Check starts {@link #RUNNING} (ADR-CHK-004). Adding a value is a new
 * CHK version.
 *
 * <p>The two unfinished values are also {@link ActiveCheckStatus}, the only values an Active Check
 * may hold (RULE-CHK-009).
 */
public enum CheckStatus {

    AWAITING_DOCUMENTS,
    RUNNING,
    COMPLETED,
    FAILED;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<CheckStatus> fromStored(String storedValue) {
        for (CheckStatus status : values()) {
            if (status.storedValue().equals(storedValue)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
