package io.agenticai.rpt.contract;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * A Check and, once COMPLETED, its report (CON-RPT-003; API-RPT-001 response schema
 * {@code CheckReport}, no envelope). The closed lookups travel as their codes. The three lists are
 * unmodifiable and empty until the Check is COMPLETED; a FAILED Check carries its reason and
 * detail and no Overall Status (REQ-RPT-026). Every text is returned exactly as stored, as data
 * (REQ-RPT-027).
 *
 * @param checkId         DBF-RPT-001 — the Check identifier
 * @param status          DBF-RPT-007 — CHECK_STATUS code
 * @param serviceCode     DBF-RPT-002
 * @param versionNumber   DBF-RPT-003
 * @param fetchMode       DBF-RPT-004 — FETCH_MODE code
 * @param requestNumber   DBF-RPT-005 — exactly as sent
 * @param employeeId      DBF-RPT-006 — exactly as sent
 * @param startedAt       DBF-RPT-008
 * @param runningSince    DBF-RPT-009 — {@code null} until first marked RUNNING
 * @param endedAt         DBF-RPT-010 — {@code null} until COMPLETED or FAILED
 * @param overallStatus   DBF-RPT-011 — OVERALL_STATUS code, COMPLETED only
 * @param comparisonModel DBF-RPT-012 — COMPLETED only
 * @param failureReason   DBF-RPT-013 — CHECK_FAILURE_REASON code, FAILED only
 * @param failureDetail   DBF-RPT-014 — FAILED only
 * @param findings        the findings, in report order
 * @param documents       the document outcomes, in report order
 * @param unreadQueries   the unread service queries, in report order
 * @param decision        the Employee Decision, or {@code null} when none is recorded
 */
public record CheckReport(Long checkId,
                          String status,
                          String serviceCode,
                          int versionNumber,
                          String fetchMode,
                          String requestNumber,
                          String employeeId,
                          OffsetDateTime startedAt,
                          OffsetDateTime runningSince,
                          OffsetDateTime endedAt,
                          String overallStatus,
                          String comparisonModel,
                          String failureReason,
                          String failureDetail,
                          List<FindingView> findings,
                          List<DocumentView> documents,
                          List<UnreadQueryView> unreadQueries,
                          DecisionView decision) {

    public CheckReport {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(fetchMode, "fetchMode");
        Objects.requireNonNull(requestNumber, "requestNumber");
        Objects.requireNonNull(employeeId, "employeeId");
        Objects.requireNonNull(startedAt, "startedAt");
        findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
        documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
        unreadQueries = List.copyOf(Objects.requireNonNull(unreadQueries, "unreadQueries"));
    }
}
