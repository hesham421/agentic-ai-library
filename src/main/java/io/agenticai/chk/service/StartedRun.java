package io.agenticai.chk.service;

import io.agenticai.chk.domain.ActiveCheckStatus;

import java.util.Objects;

/**
 * What the committed start of a Check hands its facade: the Check identifier, its initial status
 * and — for a RUNNING Check only — the working data its pipeline runs on (CON-CHK-004; SVC-API
 * startCheck step 6 — the pipeline is submitted after the commit, REQ-CHK-001). Module-internal.
 *
 * @param checkId the Check identifier
 * @param status  the initial status
 * @param context the pipeline's working data; {@code null} for an AWAITING_DOCUMENTS Check
 */
record StartedRun(Long checkId, ActiveCheckStatus status, CheckContext context) {

    StartedRun {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
    }
}
