package io.agenticai.chk.domain;

import java.util.Optional;

/**
 * The status of an Active Check (ENT-CHK-001, DBF-CHK-003; ADR-CHK-015): only the two unfinished
 * values of {@link CheckStatus} — RULE-CHK-009, enforced also by the column's CHECK constraint
 * {@code CHK_CHK_ACTIVE_CHECK_CHECK_STATUS}. A finished Check has no Active Check. The stored
 * values are the constant names, exactly the CHECK constraint's values.
 */
public enum ActiveCheckStatus {

    AWAITING_DOCUMENTS,
    RUNNING;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<ActiveCheckStatus> fromStored(String storedValue) {
        for (ActiveCheckStatus status : values()) {
            if (status.storedValue().equals(storedValue)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
