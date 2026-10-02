package io.agenticai.chk.contract;

import java.util.Objects;

/**
 * CON-CHK-005 — what confirming the uploads of a {@code manual} Check answers.
 *
 * @param checkId the Check identifier
 * @param status  the CHECK_STATUS code (CON-CHK-001) — always {@code RUNNING}
 */
public record ConfirmedCheck(Long checkId, String status) {

    public ConfirmedCheck {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
    }
}
