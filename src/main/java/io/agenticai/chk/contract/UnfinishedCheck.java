package io.agenticai.chk.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * CON-CHK-011 — one unfinished Check as the result port lists it.
 *
 * @param checkId   the Check identifier
 * @param status    {@code AWAITING_DOCUMENTS} or {@code RUNNING} (CON-CHK-001)
 * @param startedAt the start time
 */
public record UnfinishedCheck(Long checkId, String status, OffsetDateTime startedAt) {

    public UnfinishedCheck {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
    }
}
