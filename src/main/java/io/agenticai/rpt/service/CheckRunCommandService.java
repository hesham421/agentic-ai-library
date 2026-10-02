package io.agenticai.rpt.service;

import io.agenticai.rpt.contract.CheckNotFoundException;
import io.agenticai.rpt.contract.RecordedDecision;
import io.agenticai.rpt.contract.ReportNotStoredException;
import io.agenticai.rpt.contract.RptRefusalException;
import io.agenticai.rpt.domain.CheckFailure;
import io.agenticai.rpt.domain.CheckFailureReason;
import io.agenticai.rpt.domain.CheckRunOpening;
import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.domain.CheckStatusTransition;
import io.agenticai.rpt.domain.DecisionRecording;
import io.agenticai.rpt.domain.FetchMode;
import io.agenticai.rpt.domain.OverallStatus;
import io.agenticai.rpt.domain.RuleRefusal;
import io.agenticai.rpt.domain.SubmittedReport;
import io.agenticai.rpt.entity.CheckDocument;
import io.agenticai.rpt.entity.CheckRun;
import io.agenticai.rpt.entity.Finding;
import io.agenticai.rpt.entity.UnreadQuery;
import io.agenticai.rpt.repository.CheckDocumentRepository;
import io.agenticai.rpt.repository.CheckRunRepository;
import io.agenticai.rpt.repository.FindingRepository;
import io.agenticai.rpt.repository.UnreadQueryRepository;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The writes of the Check result port and the Employee Decision (SVC-API service layer, ADR-RPT-012):
 * {@code createCheckRun}, {@code markRunning}, {@code completeCheck}, {@code failCheck} and
 * {@code recordDecision}. Every method joins the caller's transaction ({@code REQUIRED}); every rule
 * is decided by its domain class on the received values (and, for the completion, on the row locked
 * {@code FOR UPDATE}) before the first write (E.5.5). A refusal is an {@link RptRefusalException}
 * declared {@code noRollbackFor}, so the caller's transaction stays usable; a database failure while
 * the report is written is a {@link ReportNotStoredException} declared {@code rollbackFor}, which
 * rolls the caller's transaction back whole (REQ-RPT-009). Status changes and the decision are
 * conditional UPDATEs; when one updates 0 rows the row is re-read and the domain class names the
 * refusal. Timestamps are the ones handed over, except {@code DECIDED_AT} and {@code UPDATED_AT}
 * ({@code SYSTIMESTAMP}). Module-internal: reached through {@code CheckResultStore} and
 * {@code ReportStoreService}.
 */
@Service
public class CheckRunCommandService {

    private static final Logger log = LoggerFactory.getLogger(CheckRunCommandService.class);

    private final CheckRunRepository checkRuns;
    private final FindingRepository findings;
    private final CheckDocumentRepository checkDocuments;
    private final UnreadQueryRepository unreadQueries;

    public CheckRunCommandService(CheckRunRepository checkRuns,
                                  FindingRepository findings,
                                  CheckDocumentRepository checkDocuments,
                                  UnreadQueryRepository unreadQueries) {
        this.checkRuns = Objects.requireNonNull(checkRuns, "checkRuns");
        this.findings = Objects.requireNonNull(findings, "findings");
        this.checkDocuments = Objects.requireNonNull(checkDocuments, "checkDocuments");
        this.unreadQueries = Objects.requireNonNull(unreadQueries, "unreadQueries");
    }

    /**
     * {@code createCheckRun} (REQ-RPT-001 … REQ-RPT-004, REQ-RPT-015, REQ-RPT-030): 1 RULE-RPT-001,
     * 2 RULE-RPT-006 (creation status), 3 RULE-RPT-002 — decided by {@link CheckRunOpening}; 4 the
     * INSERT, every result and decision column NULL, the identifier by the identity clause. A second
     * Check of the same request is simply another row.
     *
     * @return the new Check identifier
     */
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public Long createCheckRun(String serviceCode, Integer versionNumber, FetchMode fetchMode,
                               String requestNumber, String employeeId, CheckStatus status,
                               OffsetDateTime startedAt) {
        CheckRunOpening opening = CheckRunOpening.create(serviceCode, versionNumber, fetchMode,
                requestNumber, employeeId, status, startedAt);
        RefusalExceptions.raise(opening.refusal(), null);

        CheckRun stored = checkRuns.saveAndFlush(CheckRun.created(serviceCode, versionNumber, fetchMode,
                requestNumber, employeeId, status, startedAt));
        log.info("RPT check run created checkId={} status={} fetchMode={}",
                stored.getCheckRunId(), status.storedValue(), fetchMode.storedValue());
        return stored.getCheckRunId();
    }

    /**
     * {@code markRunning} (REQ-RPT-005 … REQ-RPT-007): 1 the conditional UPDATE (keeps the first
     * RUNNING_SINCE); 2 0 rows → not found, or {@link CheckStatusTransition} names the refusal
     * (RPT-409-CHECK-ENDED).
     */
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public void markRunning(Long checkId, OffsetDateTime runningSince) {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(runningSince, "runningSince");
        if (checkRuns.markRunning(checkId, runningSince) == 0) {
            throw refusalAfterNoUpdate(checkId, CheckStatusTransition::refusalToMarkRunning);
        }
        log.info("RPT check run marked running checkId={}", checkId);
    }

    /**
     * {@code completeCheck} (REQ-RPT-008 … REQ-RPT-017, REQ-RPT-019, REQ-RPT-020, REQ-RPT-053):
     * 1 the Check Run locked {@code FOR UPDATE} — not found / {@link CheckStatusTransition}
     * (RULE-RPT-003); 2 the whole report decided in memory by {@link SubmittedReport} (RULE-RPT-006,
     * 004, 007, 008, 005, in that order); 3 the conditional UPDATE, then one row per finding,
     * document outcome and unread query, positions from 1 in the order received; 4 a database
     * failure in step 3 → {@link ReportNotStoredException}, rolling the caller's transaction back.
     */
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public void completeCheck(Long checkId,
                              OverallStatus overallStatus,
                              List<SubmittedReport.FindingItem> reportFindings,
                              List<SubmittedReport.DocumentItem> documentOutcomes,
                              List<SubmittedReport.UnreadQueryItem> reportUnreadQueries,
                              SubmittedReport.Metadata metadata) {
        Objects.requireNonNull(checkId, "checkId");

        // 1 — the locking read (QR-RPT-001 FOR UPDATE), RULE-RPT-003
        CheckRun run = checkRuns.findForUpdate(checkId).orElseThrow(() -> new CheckNotFoundException(checkId));
        RefusalExceptions.raise(CheckStatusTransition.from(run.getCheckStatus()).refusalToComplete(), checkId);

        // 2 — the whole report, in memory, before any write
        SubmittedReport report = SubmittedReport.create(overallStatus, reportFindings, documentOutcomes,
                reportUnreadQueries, metadata);
        RefusalExceptions.raise(report.refusalAgainst(new SubmittedReport.StoredRun(run.getServiceCode(),
                run.getVersionNumber(), run.getFetchMode(), run.getEmployeeId(), run.getStartedAt())), checkId);

        // 3 — persist; 4 — a database failure rolls the caller's transaction back
        try {
            if (checkRuns.complete(checkId, report.overallStatus().storedValue(),
                    report.metadata().comparisonModel(), report.metadata().endedAt()) != 1) {
                throw new IllegalStateException(
                        "Check run " + checkId + " locked RUNNING was not updated to COMPLETED");
            }
            findings.saveAllAndFlush(findingRows(checkId, report.findings()));
            checkDocuments.saveAllAndFlush(documentRows(checkId, report.documents()));
            unreadQueries.saveAllAndFlush(unreadQueryRows(checkId, report.unreadQueries()));
        } catch (DataAccessException | PersistenceException databaseFailure) {
            throw new ReportNotStoredException(checkId, databaseFailure);
        }
        log.info("RPT check run completed checkId={} overallStatus={} findings={} documents={} unreadQueries={}",
                checkId, report.overallStatus().storedValue(), report.findings().size(),
                report.documents().size(), report.unreadQueries().size());
    }

    /**
     * {@code failCheck} (REQ-RPT-018, REQ-RPT-051, REQ-RPT-053): 1 RULE-RPT-010 decided by
     * {@link CheckFailure} (no row is read before the UPDATE, so end-after-start is backstopped by
     * {@code CHK_RPT_CHECK_RUN_ENDED_AT}); 2 the conditional UPDATE — OVERALL_STATUS and
     * COMPARISON_MODEL stay NULL, no Finding is written; 3 0 rows → as {@link #markRunning}.
     */
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public void failCheck(Long checkId, CheckFailureReason failureReason, String detail, OffsetDateTime endedAt) {
        Objects.requireNonNull(checkId, "checkId");
        RefusalExceptions.raise(CheckFailure.create(failureReason, detail, endedAt).refusal(null), checkId);

        if (checkRuns.fail(checkId, failureReason.storedValue(), detail, endedAt) == 0) {
            throw refusalAfterNoUpdate(checkId, CheckStatusTransition::refusalToFail);
        }
        log.info("RPT check run failed checkId={} failureReason={}", checkId, failureReason.storedValue());
    }

    /**
     * {@code recordDecision} (CON-RPT-006; REQ-RPT-032 … REQ-RPT-039): 1 RULE-RPT-013 and 2
     * RULE-RPT-014 decided by {@link DecisionRecording} on the values received; 3 the conditional
     * UPDATE ({@code DECIDED_AT = SYSTIMESTAMP}, ADR-RPT-009) — nothing else of the row or its report
     * changes; 4 0 rows → not found, or {@link DecisionRecording#refusalOn} names the refusal
     * (RULE-RPT-011 before RULE-RPT-012). The answer is read back so {@code decidedAt} is the stored
     * recording time.
     */
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public RecordedDecision recordDecision(Long checkId, String employeeDecision, String decidedBy,
                                           Boolean approvalApiExecuted) {
        Objects.requireNonNull(checkId, "checkId");
        DecisionRecording decision = DecisionRecording.create(employeeDecision, decidedBy, approvalApiExecuted);
        RefusalExceptions.raise(decision.refusal(), checkId);

        int updated = checkRuns.recordDecision(checkId, decision.decision().storedValue(),
                decision.decidedBy(), decision.approvalApiExecuted() ? 1 : 0);
        if (updated == 0) {
            CheckRun run = checkRuns.findById(checkId).orElseThrow(() -> new CheckNotFoundException(checkId));
            Optional<RuleRefusal> refusal = DecisionRecording.refusalOn(run.getCheckStatus(), run.getEmployeeDecision());
            RefusalExceptions.raise(refusal, checkId);
            throw new IllegalStateException("Check run " + checkId + " takes a decision but was not updated");
        }

        CheckRun recorded = checkRuns.findById(checkId)
                .orElseThrow(() -> new IllegalStateException("Check run " + checkId + " vanished after its decision"));
        log.info("RPT decision recorded checkId={} employeeDecision={} approvalApiExecuted={}",
                checkId, recorded.getEmployeeDecision().storedValue(), recorded.getApprovalApiExecuted());
        return new RecordedDecision(checkId, recorded.getEmployeeDecision().storedValue(),
                recorded.getDecidedBy(), recorded.getDecidedAt(), recorded.getApprovalApiExecuted());
    }

    // A conditional UPDATE changed 0 rows: re-read (QR-RPT-001) and let the state machine name why.
    private RptRefusalException refusalAfterNoUpdate(
            Long checkId, Function<CheckStatusTransition, Optional<RuleRefusal>> transition) {
        CheckRun run = checkRuns.findById(checkId).orElseThrow(() -> new CheckNotFoundException(checkId));
        return transition.apply(CheckStatusTransition.from(run.getCheckStatus()))
                .map(refusal -> RefusalExceptions.of(refusal, checkId))
                .orElseThrow(() -> new IllegalStateException("Check run " + checkId + " in status "
                        + run.getCheckStatus().storedValue() + " allows the change but was not updated"));
    }

    private static List<Finding> findingRows(Long checkId, List<SubmittedReport.FindingItem> items) {
        List<Finding> rows = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            SubmittedReport.FindingItem item = items.get(i);
            rows.add(Finding.recorded(checkId, i + 1, item.condition(), item.outcome(), item.evidence(), item.note()));
        }
        return rows;
    }

    private static List<CheckDocument> documentRows(Long checkId, List<SubmittedReport.DocumentItem> items) {
        List<CheckDocument> rows = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            SubmittedReport.DocumentItem item = items.get(i);
            rows.add(CheckDocument.recorded(checkId, i + 1, item.documentType(), item.sourceMode(),
                    item.readStatus(), item.reason(), item.detail()));
        }
        return rows;
    }

    private static List<UnreadQuery> unreadQueryRows(Long checkId, List<SubmittedReport.UnreadQueryItem> items) {
        List<UnreadQuery> rows = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            SubmittedReport.UnreadQueryItem item = items.get(i);
            rows.add(UnreadQuery.recorded(checkId, i + 1, item.queryName(), item.detail()));
        }
        return rows;
    }
}
