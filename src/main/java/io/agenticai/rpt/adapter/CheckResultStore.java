package io.agenticai.rpt.adapter;

import io.agenticai.chk.contract.CheckResultPort;
import io.agenticai.chk.contract.CheckRun;
import io.agenticai.chk.contract.ReportDocumentOutcome;
import io.agenticai.chk.contract.ReportFinding;
import io.agenticai.chk.contract.ReportMetadata;
import io.agenticai.chk.contract.ReportUnreadQuery;
import io.agenticai.chk.contract.UnfinishedCheck;
import io.agenticai.rpt.contract.ReportNotStoredException;
import io.agenticai.rpt.contract.RptRefusalException;
import io.agenticai.rpt.contract.UnknownCodeException;
import io.agenticai.rpt.domain.CheckFailureReason;
import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.domain.DocumentReadStatus;
import io.agenticai.rpt.domain.FetchMode;
import io.agenticai.rpt.domain.FindingOutcome;
import io.agenticai.rpt.domain.OverallStatus;
import io.agenticai.rpt.domain.SubmittedReport;
import io.agenticai.rpt.domain.UnreadableReason;
import io.agenticai.rpt.service.CheckRunCommandService;
import io.agenticai.rpt.service.CheckRunQueryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * RPT's inbound adapter: the bean that implements the Check Engine's result port
 * {@link CheckResultPort} (PORTS; ADR-RPT-001; CON-CHK-006 … CON-CHK-011) — the ONLY RPT class that
 * imports {@code io.agenticai.chk.contract}, and only the port and its value types. Each method
 * delegates to {@link CheckRunCommandService} / {@link CheckRunQueryService}; the Check Engine never
 * reads RPT tables and RPT never calls the Check Engine. It is RPT's only adapter: RPT runs no host
 * query, fetches no document and calls no model (REQ-RPT-047, REQ-RPT-048, REQ-RPT-049).
 *
 * <p><b>Codes.</b> CHK's closed lookups arrive as their String codes and are turned into RPT's own
 * enums with {@code fromStored}: an absent or blank code is an absent value (decided by the domain
 * — RULE-RPT-001, RULE-RPT-006's "null enum on a required field", RULE-RPT-008, RULE-RPT-010); a
 * code outside its closed list is refused here with {@link UnknownCodeException}
 * (RPT-422-UNKNOWN-CODE, RULE-RPT-006), before anything is written.
 *
 * <p><b>Closed value types</b> (REQ-RPT-053, ADR-RPT-018): {@code completeCheck} and
 * {@code failCheck} accept only the Check Engine's records, whose components are exactly the
 * CON-CHK-008 / CON-CHK-009 fields; they are copied component by component into RPT's own records
 * ({@link SubmittedReport}), which have no map, byte-array or untyped component — an undeclared
 * field cannot be carried and nothing of it is written.
 *
 * <p><b>Transactions</b> (ADR-RPT-012): every method joins the caller's transaction
 * ({@code REQUIRED}). A refusal ({@link RptRefusalException}) does not roll it back, so the Check
 * Engine can still fail the Check in it; a database failure while the report is written
 * ({@link ReportNotStoredException}) rolls it back whole.
 */
@Component("rptCheckResultStore")
public class CheckResultStore implements CheckResultPort {

    private final CheckRunCommandService commands;
    private final CheckRunQueryService queries;

    public CheckResultStore(CheckRunCommandService commands, CheckRunQueryService queries) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.queries = Objects.requireNonNull(queries, "queries");
    }

    /** CON-CHK-006 — delegates to {@link CheckRunCommandService#createCheckRun}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public Long createCheckRun(String serviceCode, int versionNumber, String fetchMode, String requestNumber,
                               String employeeId, String status, OffsetDateTime startedAt) {
        return commands.createCheckRun(serviceCode, versionNumber,
                code(fetchMode, FetchMode::fromStored, "FETCH_MODE"),
                requestNumber, employeeId,
                code(status, CheckStatus::fromStored, "CHECK_STATUS"),
                startedAt);
    }

    /** CON-CHK-007 — delegates to {@link CheckRunCommandService#markRunning}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public void markRunning(Long checkId, OffsetDateTime runningSince) {
        commands.markRunning(checkId, runningSince);
    }

    /** CON-CHK-008 — delegates to {@link CheckRunCommandService#completeCheck}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public void completeCheck(Long checkId, String overallStatus, List<ReportFinding> findings,
                              List<ReportDocumentOutcome> documentOutcomes, List<ReportUnreadQuery> unreadQueries,
                              ReportMetadata metadata) {
        Objects.requireNonNull(findings, "findings");
        Objects.requireNonNull(documentOutcomes, "documentOutcomes");
        Objects.requireNonNull(unreadQueries, "unreadQueries");
        commands.completeCheck(checkId,
                code(overallStatus, OverallStatus::fromStored, "OVERALL_STATUS"),
                findings.stream().map(CheckResultStore::finding).toList(),
                documentOutcomes.stream().map(CheckResultStore::document).toList(),
                unreadQueries.stream().map(CheckResultStore::unreadQuery).toList(),
                metadata == null ? null : metadata(metadata));
    }

    /** CON-CHK-009 — delegates to {@link CheckRunCommandService#failCheck}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public void failCheck(Long checkId, String failureReason, String detail, OffsetDateTime endedAt) {
        commands.failCheck(checkId,
                code(failureReason, CheckFailureReason::fromStored, "CHECK_FAILURE_REASON"),
                detail, endedAt);
    }

    /** CON-CHK-010 — delegates to {@link CheckRunQueryService#findCheck}; not found is empty. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public Optional<CheckRun> getCheck(Long checkId) {
        return queries.findCheck(checkId).map(run -> new CheckRun(run.checkId(), run.status().storedValue(),
                run.serviceCode(), run.versionNumber(), run.fetchMode().storedValue(), run.requestNumber(),
                run.employeeId(), run.startedAt()));
    }

    /** CON-CHK-011 — delegates to {@link CheckRunQueryService#listUnfinishedChecks}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<UnfinishedCheck> listUnfinishedChecks() {
        return queries.listUnfinishedChecks().stream()
                .map(row -> new UnfinishedCheck(row.checkRunId(), row.checkStatus().storedValue(), row.startedAt()))
                .toList();
    }

    private static SubmittedReport.FindingItem finding(ReportFinding finding) {
        return new SubmittedReport.FindingItem(finding.condition(),
                code(finding.outcome(), FindingOutcome::fromStored, "FINDING_OUTCOME"),
                finding.evidence(), finding.note());
    }

    private static SubmittedReport.DocumentItem document(ReportDocumentOutcome outcome) {
        return new SubmittedReport.DocumentItem(outcome.documentType(),
                code(outcome.sourceMode(), FetchMode::fromStored, "FETCH_MODE"),
                code(outcome.readStatus(), DocumentReadStatus::fromStored, "DOCUMENT_READ_STATUS"),
                code(outcome.reason(), UnreadableReason::fromStored, "UNREADABLE_REASON"),
                outcome.detail());
    }

    private static SubmittedReport.UnreadQueryItem unreadQuery(ReportUnreadQuery query) {
        return new SubmittedReport.UnreadQueryItem(query.queryName(), query.detail());
    }

    private static SubmittedReport.Metadata metadata(ReportMetadata metadata) {
        return new SubmittedReport.Metadata(metadata.serviceCode(), metadata.versionNumber(),
                code(metadata.fetchMode(), FetchMode::fromStored, "FETCH_MODE"),
                metadata.comparisonModel(), metadata.employeeId(), metadata.startedAt(), metadata.endedAt());
    }

    /**
     * The RPT enum of a carried code: absent or blank → {@code null} (the domain decides what an
     * absent value means); outside the closed list → RULE-RPT-006.
     */
    private static <E> E code(String value, Function<String, Optional<E>> fromStored, String lookupKey) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return fromStored.apply(value).orElseThrow(() -> new UnknownCodeException(value, lookupKey));
    }
}
