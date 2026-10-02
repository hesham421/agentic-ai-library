package io.agenticai.chk.contract;

import java.util.Objects;

/**
 * CON-CHK-004 — what starting a Check answers: the Check identifier the result port allocated and
 * the Check's initial status.
 *
 * @param checkId the Check identifier ({@code NUMBER(19)}, RPT's)
 * @param status  the CHECK_STATUS code (CON-CHK-001): {@code RUNNING} for a {@code path} /
 *                {@code blob} service, {@code AWAITING_DOCUMENTS} for a {@code manual} one
 */
public record StartedCheck(Long checkId, String status) {

    public StartedCheck {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
    }
}
