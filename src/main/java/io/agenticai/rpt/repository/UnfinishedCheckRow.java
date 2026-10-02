package io.agenticai.rpt.repository;

import io.agenticai.rpt.domain.CheckStatus;

import java.time.OffsetDateTime;

/**
 * The projection CHECK_RUN_ID, CHECK_STATUS, STARTED_AT of the result port's
 * {@code listUnfinishedChecks} read (SVC-API inline operation; CON-CHK-011, REQ-RPT-022).
 * Module-internal.
 */
public record UnfinishedCheckRow(Long checkRunId, CheckStatus checkStatus, OffsetDateTime startedAt) {
}
