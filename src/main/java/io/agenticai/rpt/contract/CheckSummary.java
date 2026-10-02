package io.agenticai.rpt.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * One Check of a request as the listing shows it (CON-RPT-004; API-RPT-002 schema
 * {@code CheckSummary}). Each Check is its own entry; nothing is merged across Checks
 * (REQ-RPT-030).
 *
 * @param checkId          DBF-RPT-001
 * @param status           DBF-RPT-007 — CHECK_STATUS code
 * @param overallStatus    DBF-RPT-011 — OVERALL_STATUS code, or {@code null}
 * @param startedAt        DBF-RPT-008
 * @param endedAt          DBF-RPT-010 — or {@code null}
 * @param employeeDecision DBF-RPT-015 — EMPLOYEE_DECISION code, or {@code null}
 */
public record CheckSummary(Long checkId,
                           String status,
                           String overallStatus,
                           OffsetDateTime startedAt,
                           OffsetDateTime endedAt,
                           String employeeDecision) {

    public CheckSummary {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
    }
}
