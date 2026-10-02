package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * One row of the decision agreement of a service (CON-RPT-005; API-RPT-003 schema
 * {@code AgreementRow}): how many decided Checks of a version and Overall Status were approved or
 * rejected (REQ-RPT-040).
 *
 * @param versionNumber    DBF-RPT-003
 * @param overallStatus    DBF-RPT-011 — OVERALL_STATUS code
 * @param employeeDecision DBF-RPT-015 — EMPLOYEE_DECISION code
 * @param count            the number of decided Checks, 1 or more
 */
public record AgreementRow(int versionNumber, String overallStatus, String employeeDecision, long count) {

    public AgreementRow {
        Objects.requireNonNull(overallStatus, "overallStatus");
        Objects.requireNonNull(employeeDecision, "employeeDecision");
    }
}
