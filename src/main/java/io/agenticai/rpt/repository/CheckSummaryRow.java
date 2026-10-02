package io.agenticai.rpt.repository;

import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.domain.EmployeeDecision;
import io.agenticai.rpt.domain.OverallStatus;

import java.time.OffsetDateTime;

/**
 * The projection of QR-RPT-005 — CHECK_RUN_ID, CHECK_STATUS, OVERALL_STATUS, STARTED_AT, ENDED_AT,
 * EMPLOYEE_DECISION of one Check of a request. Module-internal.
 */
public record CheckSummaryRow(Long checkRunId,
                              CheckStatus checkStatus,
                              OverallStatus overallStatus,
                              OffsetDateTime startedAt,
                              OffsetDateTime endedAt,
                              EmployeeDecision employeeDecision) {
}
