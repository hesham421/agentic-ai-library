package io.agenticai.chk.service;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * What the committed upload confirmation hands step 4: the Check and the end of its Check timeout,
 * counted from the confirmation (REQ-CHK-052, REQ-CHK-077). Module-internal.
 *
 * @param checkId    the Check identifier
 * @param deadlineAt the deadline written to the Active Check
 */
record ConfirmedRun(Long checkId, OffsetDateTime deadlineAt) {

    ConfirmedRun {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(deadlineAt, "deadlineAt");
    }
}
