package io.agenticai.chk.repository;

import io.agenticai.chk.domain.ActiveCheckStatus;

import java.time.OffsetDateTime;

/**
 * The projection CHECK_ID, CHECK_STATUS, DEADLINE_AT of one Active Check (QR-CHK-001; the deadline
 * check's read) — never the key or the audit fields. Module-internal.
 *
 * @param checkId     DBF-CHK-002
 * @param checkStatus DBF-CHK-003
 * @param deadlineAt  DBF-CHK-004
 */
public record ActiveCheckRow(Long checkId, ActiveCheckStatus checkStatus, OffsetDateTime deadlineAt) {
}
