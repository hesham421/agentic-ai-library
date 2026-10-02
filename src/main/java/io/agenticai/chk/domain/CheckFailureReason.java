package io.agenticai.chk.domain;

import java.util.Optional;

/**
 * The closed reason a Check ended FAILED (CON-CHK-003; ADR-CHK-001, ADR-CHK-005, ADR-CHK-006). The
 * stored values are the constant names, which a consumer stores as {@code VARCHAR2(30 CHAR)}.
 * Every FAILED Check carries exactly one reason; no other Check carries one (REQ-CHK-054). Adding a
 * value is a new CHK version.
 *
 * <ul>
 *   <li>{@link #TIMED_OUT} — RUNNING longer than {@code aias.check.timeout} (REQ-CHK-051)</li>
 *   <li>{@link #MODEL_UNAVAILABLE} — the comparison model cannot be reached or is not
 *       configured</li>
 *   <li>{@link #MODEL_OUTPUT_INVALID} — the structured output does not fit the fixed report
 *       structure</li>
 *   <li>{@link #MODEL_NOT_PERMITTED} — a FREE-tier comparison model with REAL data
 *       (REQ-CHK-072)</li>
 *   <li>{@link #UPLOAD_WINDOW_EXPIRED} — AWAITING_DOCUMENTS longer than
 *       {@code aias.check.upload-window} (REQ-CHK-060)</li>
 *   <li>{@link #INTERRUPTED} — left unfinished by an earlier run of the service (REQ-CHK-055)</li>
 *   <li>{@link #INTERNAL_ERROR} — any other failure, including a result that could not be stored
 *       (REQ-CHK-048)</li>
 * </ul>
 */
public enum CheckFailureReason {

    TIMED_OUT,
    MODEL_UNAVAILABLE,
    MODEL_OUTPUT_INVALID,
    MODEL_NOT_PERMITTED,
    UPLOAD_WINDOW_EXPIRED,
    INTERRUPTED,
    INTERNAL_ERROR;

    /** The value as a consumer stores it — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<CheckFailureReason> fromStored(String storedValue) {
        for (CheckFailureReason reason : values()) {
            if (reason.storedValue().equals(storedValue)) {
                return Optional.of(reason);
            }
        }
        return Optional.empty();
    }
}
