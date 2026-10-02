package io.agenticai.integration.dto;

import io.agenticai.rpt.contract.CheckReport;
import io.agenticai.rpt.contract.DecisionView;
import io.agenticai.rpt.contract.DocumentView;
import io.agenticai.rpt.contract.FindingView;
import io.agenticai.rpt.contract.UnreadQueryView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * The API document's {@code CheckReportResponse} schema (API-INT-005), field for field — the
 * Report Store's read-a-Check answer, unchanged; every text as stored, as data (REQ-INT-061). INT
 * derives nothing (the Overall Status is the stored one — ADR-INT-011 (2)). Nullable fields are
 * serialised as {@code null}, as the document's {@code [string, 'null']} types allow.
 *
 * @param checkId         DBF-INT-001 — integer int64
 * @param status          DBF-INT-002 — CHECK_STATUS code
 * @param serviceCode     DBF-INT-003
 * @param versionNumber   DBF-INT-004
 * @param fetchMode       FETCH_MODE code
 * @param requestNumber   DBF-INT-005 — exactly as sent
 * @param employeeId      DBF-INT-006 — exactly as sent
 * @param startedAt       ISO-8601 date-time
 * @param runningSince    ISO-8601 date-time, nullable
 * @param endedAt         ISO-8601 date-time, nullable
 * @param overallStatus   OVERALL_STATUS code, nullable (COMPLETED only)
 * @param comparisonModel nullable (COMPLETED only)
 * @param failureReason   CHECK_FAILURE_REASON code, nullable (FAILED only)
 * @param failureDetail   nullable (FAILED only)
 * @param findings        the findings in report order, unmodifiable
 * @param documents       the document outcomes in report order, unmodifiable
 * @param unreadQueries   the unread queries in report order, unmodifiable
 * @param decision        the Employee Decision, or {@code null} when none is recorded
 */
public record CheckReportResponse(Long checkId,
                                  String status,
                                  String serviceCode,
                                  Integer versionNumber,
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
                                  List<Finding> findings,
                                  List<Document> documents,
                                  List<UnreadQuery> unreadQueries,
                                  Decision decision) {

    public CheckReportResponse {
        findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
        documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
        unreadQueries = List.copyOf(Objects.requireNonNull(unreadQueries, "unreadQueries"));
    }

    /**
     * One finding.
     *
     * @param position  the position in the report
     * @param condition the condition text, as stored
     * @param outcome   FINDING_OUTCOME code
     * @param evidence  as stored, as data
     * @param note      as stored
     */
    public record Finding(int position, String condition, String outcome, String evidence, String note) {

        static Finding of(FindingView view) {
            return new Finding(view.position(), view.condition(), view.outcome(), view.evidence(), view.note());
        }
    }

    /**
     * One document outcome.
     *
     * @param position         the position in the report
     * @param documentType     the document type code, string ≤ 100
     * @param sourceMode       FETCH_MODE code
     * @param readStatus       DOCUMENT_READ_STATUS code
     * @param unreadableReason nullable
     * @param detail           nullable
     */
    public record Document(int position, String documentType, String sourceMode, String readStatus,
                           String unreadableReason, String detail) {

        static Document of(DocumentView view) {
            return new Document(view.position(), view.documentType(), view.sourceMode(), view.readStatus(),
                    view.unreadableReason(), view.detail());
        }
    }

    /**
     * One unread query.
     *
     * @param position  the position in the report
     * @param queryName string ≤ 100
     * @param detail    as stored
     */
    public record UnreadQuery(int position, String queryName, String detail) {

        static UnreadQuery of(UnreadQueryView view) {
            return new UnreadQuery(view.position(), view.queryName(), view.detail());
        }
    }

    /**
     * The recorded Employee Decision.
     *
     * @param employeeDecision    DBF-INT-007 — EMPLOYEE_DECISION code
     * @param decidedBy           DBF-INT-008 — exactly as sent
     * @param decidedAt           ISO-8601 date-time
     * @param approvalApiExecuted whether executed through the host Approval API
     */
    public record Decision(String employeeDecision, String decidedBy, OffsetDateTime decidedAt,
                           boolean approvalApiExecuted) {

        static Decision of(DecisionView view) {
            return view == null ? null : new Decision(view.employeeDecision(), view.decidedBy(),
                    view.decidedAt(), view.approvalApiExecuted());
        }
    }

    /** The one mapping of the Report Store's read to the response; no decision is taken here. */
    public static CheckReportResponse of(CheckReport report) {
        Objects.requireNonNull(report, "report");
        return new CheckReportResponse(
                report.checkId(),
                report.status(),
                report.serviceCode(),
                report.versionNumber(),
                report.fetchMode(),
                report.requestNumber(),
                report.employeeId(),
                report.startedAt(),
                report.runningSince(),
                report.endedAt(),
                report.overallStatus(),
                report.comparisonModel(),
                report.failureReason(),
                report.failureDetail(),
                report.findings().stream().map(Finding::of).toList(),
                report.documents().stream().map(Document::of).toList(),
                report.unreadQueries().stream().map(UnreadQuery::of).toList(),
                Decision.of(report.decision()));
    }
}
