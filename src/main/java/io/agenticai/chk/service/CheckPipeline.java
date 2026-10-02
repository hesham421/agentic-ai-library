package io.agenticai.chk.service;

import io.agenticai.chk.config.ComparisonModelProperties;
import io.agenticai.chk.contract.ReportMetadata;
import io.agenticai.chk.domain.DocumentReading;
import io.agenticai.chk.domain.ExplicitCondition;
import io.agenticai.chk.domain.FindingVerifier;
import io.agenticai.chk.domain.OverallStatus;
import io.agenticai.chk.domain.OverallStatusDecision;
import io.agenticai.chk.domain.RequiredDocumentRule;
import io.agenticai.chk.domain.VerifiedFinding;
import io.agenticai.chk.port.CheckData;
import io.agenticai.chk.port.ComparisonModelPort;
import io.agenticai.chk.port.ComparisonOutput;
import io.agenticai.chk.port.ConnectionLookup;
import io.agenticai.chk.port.DocumentOutcome;
import io.agenticai.chk.port.DocumentPort;
import io.agenticai.chk.port.QueryResult;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.port.ServiceQuery;
import io.agenticai.chk.port.ServiceQueryPort;
import io.agenticai.chk.port.VersionQuery;
import io.agenticai.platform.config.CheckLimitsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@code CheckPipeline.run(checkId, CheckContext)} — the same fixed steps, in this order, for every
 * Check (REQ-CHK-009): 1 service queries, 2 documents, 3 deterministic checks part 1 (required
 * documents, RULE-CHK-004), 4 comparison, 5 deterministic checks part 2 (verification of the
 * model's findings, ADR-CHK-003), 6 Overall Status, 7 ending. Plain sequential code — no workflow
 * engine. Orchestration only: every decision is a domain class's ({@link RequiredDocumentRule},
 * {@link FindingVerifier}, {@link OverallStatusDecision}, {@link PipelineFailure}).
 *
 * <p><b>Deadline.</b> Before every step the Check's own deadline ({@code aias.check.timeout}
 * counted from RUNNING, REQ-CHK-052) is tested; reached → FAILED / TIMED_OUT (REQ-CHK-051). The
 * deadline also bounds each external call (queries, documents, model).
 *
 * <p><b>No transaction.</b> The pipeline runs on a background thread and holds NO database
 * transaction: the queries (MCP), the documents and the model call run outside any transaction
 * (A.5.5); only the ending opens its own short one.
 *
 * <p><b>Failures.</b> Every exception of a step becomes the Check's failure through
 * {@link PipelineFailure} (REQ-CHK-010). When {@code run} returns, the {@link CheckContext} is
 * cleared and dropped (REQ-CHK-065). Nothing of the Check's data is logged at info or above.
 */
@Service
public class CheckPipeline {

    private static final Logger log = LoggerFactory.getLogger(CheckPipeline.class);

    private final ServiceQueryPort queries;
    private final ConnectionLookup connections;
    private final DocumentPort documents;
    private final ComparisonModelPort comparison;
    private final CheckLimitsProperties limits;
    private final ComparisonModelProperties comparisonModel;
    private final PipelineEnding ending;

    CheckPipeline(ServiceQueryPort queries,
                  ConnectionLookup connections,
                  DocumentPort documents,
                  ComparisonModelPort comparison,
                  CheckLimitsProperties limits,
                  ComparisonModelProperties comparisonModel,
                  PipelineEnding ending) {
        this.queries = Objects.requireNonNull(queries, "queries");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.documents = Objects.requireNonNull(documents, "documents");
        this.comparison = Objects.requireNonNull(comparison, "comparison");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.comparisonModel = Objects.requireNonNull(comparisonModel, "comparisonModel");
        this.ending = Objects.requireNonNull(ending, "ending");
    }

    /** Runs the Check to its end; never throws for a step's failure. */
    void run(CheckContext context) {
        Long checkId = context.checkId();
        try {
            runSteps(context);
        } catch (RuntimeException failure) {
            // an interrupt from the deadline check must not break the ending's own database call
            boolean interrupted = Thread.interrupted();
            PipelineFailure ended = PipelineFailure.of(failure);
            log.warn("CHK pipeline step failed checkId={} reason={} interrupted={}",
                    checkId, ended.reason().storedValue(), interrupted);
            log.debug("CHK pipeline failure checkId={}", checkId, failure);
            ending.fail(checkId, ended.reason(), ended.detail());
        } finally {
            context.clear();
        }
    }

    private void runSteps(CheckContext context) {
        Long checkId = context.checkId();
        ServicePackageSnapshot servicePackage = context.servicePackage();
        // 1 — service queries (PORTS-QUERY; REQ-CHK-011 … REQ-CHK-016, REQ-CHK-049, REQ-CHK-050)
        if (timedOut(context)) {
            return;
        }
        runQueries(context, servicePackage);
        // 2 — documents (REQ-CHK-017; REQ-CHK-023 on failure)
        if (timedOut(context)) {
            return;
        }
        context.documents(documents.fetch(checkId, context.requestNumber(), servicePackage.serviceCode(),
                servicePackage.versionNumber(), context.deadline()));
        // 3 — deterministic checks, part 1: required documents (RULE-CHK-004)
        if (timedOut(context)) {
            return;
        }
        List<VerifiedFinding> requiredDocumentFindings = RequiredDocumentRule.decide(
                servicePackage.requiredDocumentTypes(), readings(context.documents()));
        // 4 — comparison (PORTS-MODEL gate inside the port)
        if (timedOut(context)) {
            return;
        }
        CheckData data = new CheckData(context.queryResults(), context.documents());
        context.modelOutput(comparison.compare(servicePackage.serviceKnowledge(), data, context.deadline()));
        // 5 — deterministic checks, part 2: verification of the model's findings (ADR-CHK-003)
        if (timedOut(context)) {
            return;
        }
        FindingVerifier verifier = FindingVerifier.create(servicePackage.serviceKnowledge(),
                CheckDataText.of(context.queryResults(), data.readDocuments()));
        List<VerifiedFinding> findings = new ArrayList<>();
        for (ComparisonOutput.Finding finding : context.modelOutput().findings()) {
            findings.add(verifier.verify(finding.condition(), finding.outcome(), finding.evidence(),
                    finding.note(), explicitOf(finding.explicit())));
        }
        findings.addAll(requiredDocumentFindings);
        // 6 — Overall Status (REQ-CHK-041 … REQ-CHK-043)
        if (timedOut(context)) {
            return;
        }
        OverallStatus overallStatus = OverallStatusDecision.decide(
                findings.stream().map(VerifiedFinding::outcome).toList(), !context.unreadQueries().isEmpty());
        // 7 — ending (REQ-CHK-044, REQ-CHK-045)
        if (timedOut(context)) {
            return;
        }
        ending.complete(checkId, new CheckReport(overallStatus, findings, context.documents(),
                context.unreadQueries(), metadata(context, servicePackage)));
    }

    /** Step 1: every query of the version but the document source query (RULE-CHK-005). */
    private void runQueries(CheckContext context, ServicePackageSnapshot servicePackage) {
        List<VersionQuery> selected = ServiceQuerySelection.select(
                servicePackage.queries(), VersionQuery::queryName, servicePackage.documentSourceQueryName());
        for (VersionQuery query : selected) {
            ServiceQuery run = new ServiceQuery(query.queryName(), query.connectionName(), query.sqlText(),
                    servicePackage.inputName(), context.requestNumber(), limits.maxRows());
            QueryResult result = queries.run(run, connections.find(query.connectionName()).orElse(null),
                    context.deadline());
            switch (result) {
                case QueryResult.Rows rows -> context.queryResults().put(query.queryName(), rows.rows());
                case QueryResult.NotRead notRead -> context.unreadQueries().add(notRead.toUnread(query.queryName()));
            }
            log.debug("CHK query ran checkId={} query={} read={}",
                    context.checkId(), query.queryName(), result instanceof QueryResult.Rows);
        }
    }

    /** The Check's own deadline reached → FAILED / TIMED_OUT (REQ-CHK-051). */
    private boolean timedOut(CheckContext context) {
        if (!context.deadlinePassed(Instant.now())) {
            return false;
        }
        PipelineFailure timedOut = PipelineFailure.timedOut();
        log.info("CHK check timed out checkId={}", context.checkId());
        ending.fail(context.checkId(), timedOut.reason(), timedOut.detail());
        return true;
    }

    private ReportMetadata metadata(CheckContext context, ServicePackageSnapshot servicePackage) {
        return new ReportMetadata(servicePackage.serviceCode(), servicePackage.versionNumber(),
                servicePackage.fetchMode(), comparisonModel.model(), context.employeeId(),
                context.startedAt(), OffsetDateTime.now());
    }

    private static List<DocumentReading> readings(List<DocumentOutcome> outcomes) {
        return outcomes.stream()
                .map(o -> new DocumentReading(o.documentType(), o.readStatus(), o.reason(), o.detail()))
                .toList();
    }

    private static ExplicitCondition explicitOf(ComparisonOutput.Explicit explicit) {
        return explicit == null ? null
                : new ExplicitCondition(explicit.valueFound(), explicit.comparison(), explicit.limit());
    }
}
