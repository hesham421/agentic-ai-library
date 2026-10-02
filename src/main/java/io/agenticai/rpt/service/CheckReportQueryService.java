package io.agenticai.rpt.service;

import io.agenticai.rpt.contract.CheckNotFoundException;
import io.agenticai.rpt.contract.CheckReport;
import io.agenticai.rpt.contract.CheckSummary;
import io.agenticai.rpt.contract.ChecksOfRequest;
import io.agenticai.rpt.contract.DecisionView;
import io.agenticai.rpt.contract.DocumentView;
import io.agenticai.rpt.contract.FindingView;
import io.agenticai.rpt.contract.RequestKeysMissingException;
import io.agenticai.rpt.contract.UnreadQueryView;
import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.domain.ReportLimits;
import io.agenticai.rpt.entity.CheckDocument;
import io.agenticai.rpt.entity.CheckRun;
import io.agenticai.rpt.entity.Finding;
import io.agenticai.rpt.entity.UnreadQuery;
import io.agenticai.rpt.error.ReportStoreException;
import io.agenticai.rpt.repository.CheckDocumentRepository;
import io.agenticai.rpt.repository.CheckRunRepository;
import io.agenticai.rpt.repository.CheckSummaryRow;
import io.agenticai.rpt.repository.FindingRepository;
import io.agenticai.rpt.repository.UnreadQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * The two Check reads of the Report Store — API-RPT-001 / CON-RPT-003 ({@link #read}) and
 * API-RPT-002 / CON-RPT-004 ({@link #listOfRequest}); the same methods serve the HTTP controller and
 * {@code ReportStore}. READ_ONLY, one transaction per read so a report is read as one consistent
 * state; writes nothing. Every value is returned exactly as stored (REQ-RPT-027). A refusal does not
 * roll back a caller's transaction ({@code noRollbackFor}). Module-internal.
 */
@Service
public class CheckReportQueryService {

    private static final Logger log = LoggerFactory.getLogger(CheckReportQueryService.class);

    private final CheckRunRepository checkRuns;
    private final FindingRepository findings;
    private final CheckDocumentRepository checkDocuments;
    private final UnreadQueryRepository unreadQueries;

    public CheckReportQueryService(CheckRunRepository checkRuns,
                                   FindingRepository findings,
                                   CheckDocumentRepository checkDocuments,
                                   UnreadQueryRepository unreadQueries) {
        this.checkRuns = Objects.requireNonNull(checkRuns, "checkRuns");
        this.findings = Objects.requireNonNull(findings, "findings");
        this.checkDocuments = Objects.requireNonNull(checkDocuments, "checkDocuments");
        this.unreadQueries = Objects.requireNonNull(unreadQueries, "unreadQueries");
    }

    /**
     * API-RPT-001 orchestration: load the Check Run (QR-RPT-001) — none →
     * RPT-404-CHECK-NOT-FOUND (REQ-RPT-025, also a purged Check); COMPLETED → its Findings,
     * Check Documents and Unread Queries (QR-RPT-002 … QR-RPT-004) by POSITION, each finding one
     * entry with its evidence (REQ-RPT-024); otherwise the lists stay empty and a FAILED Check
     * carries its reason and detail (REQ-RPT-026).
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = ReportStoreException.class)
    public CheckReport read(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        CheckRun run = checkRuns.findById(checkId).orElseThrow(() -> new CheckNotFoundException(checkId));
        boolean completed = run.getCheckStatus() == CheckStatus.COMPLETED;
        List<FindingView> findingViews = completed
                ? findings.findOfCheckRun(checkId).stream().map(CheckReportQueryService::view).toList()
                : List.of();
        List<DocumentView> documentViews = completed
                ? checkDocuments.findOfCheckRun(checkId).stream().map(CheckReportQueryService::view).toList()
                : List.of();
        List<UnreadQueryView> unreadQueryViews = completed
                ? unreadQueries.findOfCheckRun(checkId).stream().map(CheckReportQueryService::view).toList()
                : List.of();
        log.debug("RPT read check report checkId={} status={}", checkId, run.getCheckStatus().storedValue());
        return new CheckReport(
                run.getCheckRunId(),
                run.getCheckStatus().storedValue(),
                run.getServiceCode(),
                run.getVersionNumber(),
                run.getFetchMode().storedValue(),
                run.getRequestNumber(),
                run.getEmployeeId(),
                run.getStartedAt(),
                run.getRunningSince(),
                run.getEndedAt(),
                run.getOverallStatus() == null ? null : run.getOverallStatus().storedValue(),
                run.getComparisonModel(),
                run.getFailureReason() == null ? null : run.getFailureReason().storedValue(),
                run.getFailureDetail(),
                findingViews,
                documentViews,
                unreadQueryViews,
                decisionOf(run));
    }

    /**
     * API-RPT-002 orchestration: RULE-RPT-009 (both keys present and not blank, else
     * RPT-400-REQUEST-KEYS-MISSING) → the newest {@link ReportLimits#LIST_LIMIT} Checks (QR-RPT-005)
     * and the total (QR-RPT-006), both keys as bound parameters, matched exactly (REQ-RPT-050).
     * Each Check is its own entry (REQ-RPT-030).
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = ReportStoreException.class)
    public ChecksOfRequest listOfRequest(String serviceCode, String requestNumber) {
        if (isBlank(serviceCode) || isBlank(requestNumber)) {
            throw new RequestKeysMissingException();
        }
        List<CheckSummary> checks = checkRuns
                .findNewestOfRequest(serviceCode, requestNumber, Limit.of(ReportLimits.LIST_LIMIT))
                .stream().map(CheckReportQueryService::summary).toList();
        long total = checkRuns.countOfRequest(serviceCode, requestNumber);
        log.debug("RPT read checks of request returned={} total={}", checks.size(), total);
        return new ChecksOfRequest(total, checks);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static DecisionView decisionOf(CheckRun run) {
        if (run.getEmployeeDecision() == null) {
            return null;
        }
        return new DecisionView(run.getEmployeeDecision().storedValue(), run.getDecidedBy(),
                run.getDecidedAt(), run.getApprovalApiExecuted());
    }

    private static FindingView view(Finding finding) {
        return new FindingView(finding.getPosition(), finding.getConditionText(),
                finding.getFindingOutcome().storedValue(), finding.getEvidence(), finding.getNote());
    }

    private static DocumentView view(CheckDocument document) {
        return new DocumentView(document.getPosition(), document.getDocumentType(),
                document.getSourceMode().storedValue(), document.getReadStatus().storedValue(),
                document.getUnreadableReason() == null ? null : document.getUnreadableReason().storedValue(),
                document.getDetail());
    }

    private static UnreadQueryView view(UnreadQuery unreadQuery) {
        return new UnreadQueryView(unreadQuery.getPosition(), unreadQuery.getQueryName(), unreadQuery.getDetail());
    }

    private static CheckSummary summary(CheckSummaryRow row) {
        return new CheckSummary(row.checkRunId(), row.checkStatus().storedValue(),
                row.overallStatus() == null ? null : row.overallStatus().storedValue(),
                row.startedAt(), row.endedAt(),
                row.employeeDecision() == null ? null : row.employeeDecision().storedValue());
    }
}
