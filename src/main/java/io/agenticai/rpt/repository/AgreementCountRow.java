package io.agenticai.rpt.repository;

import io.agenticai.rpt.domain.EmployeeDecision;
import io.agenticai.rpt.domain.OverallStatus;

/**
 * The projection of QR-RPT-007 — VERSION_NUMBER, OVERALL_STATUS, EMPLOYEE_DECISION, COUNT(*) of the
 * decided Checks of a service. Module-internal.
 */
public record AgreementCountRow(Integer versionNumber,
                                OverallStatus overallStatus,
                                EmployeeDecision employeeDecision,
                                Long count) {
}
