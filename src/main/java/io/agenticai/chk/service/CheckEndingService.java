package io.agenticai.chk.service;

import io.agenticai.chk.contract.CheckResultPort;
import io.agenticai.platform.tx.JdbcSavepoints;
import io.agenticai.chk.domain.CheckFailureReason;
import io.agenticai.chk.error.CheckEngineErrorCodes;
import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.DocumentPort;
import io.agenticai.chk.repository.ActiveCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The single ending path of every Check, COMPLETED or FAILED (SVC-API {@code complete} /
 * {@code fail}, steps 1 → 3; REQ-CHK-044, REQ-CHK-054, REQ-CHK-061, REQ-CHK-078, REQ-CHK-079).
 *
 * <p><b>Transaction.</b> Each ending is its own short READ_WRITE transaction
 * ({@code REQUIRES_NEW}, so it never joins a caller's): 1 {@code DELETE} of the Check's Active
 * Check — the guard: 0 rows means another path already ended the Check, and this one returns
 * handing nothing and notifying nothing; 2 with 1 row deleted, the result port's
 * {@code completeCheck} or {@code failCheck}. A {@code completeCheck} failure is rolled back to a
 * JDBC savepoint taken just before it and becomes {@code failCheck(INTERNAL_ERROR)} in the same
 * transaction (REQ-CHK-048). 3 After the commit, Document Access's end-of-Check notice
 * ({@link DocumentPort#endCheck} sends it once more on failure and logs a second failure,
 * REQ-CHK-062). No model, MCP or host call is made while the transaction is open (A.5.5).
 *
 * <p><b>Savepoint.</b> Taken by the platform's {@link JdbcSavepoints} on the ending transaction's
 * own JDBC connection — Spring's {@code TransactionStatus.createSavepoint()} is unsupported under
 * {@code JpaTransactionManager} with Hibernate. The savepoint rolls the report's SQL back, but it
 * cannot clear a rollback-only mark: RPT's {@code completeCheck} declares
 * {@code rollbackFor = ReportNotStoredException} (ADR-RPT-012(3)) and joins this transaction, and
 * Hibernate itself marks the transaction rollback-only on many flush failures. When that happens
 * the in-transaction {@code failCheck(INTERNAL_ERROR)} is doomed too: the commit throws
 * {@code UnexpectedRollbackException}, the DELETE is undone with it, and {@link CheckPipeline}'s
 * catch ends the Check through a fresh {@code REQUIRES_NEW} {@link #fail} — FAILED /
 * INTERNAL_ERROR (RPT audit X1; recorded as RPT's OPEN {@code api_doc_gaps} row). The in-transaction
 * fallback still serves every failure that leaves the transaction committable.
 */
@Service
public class CheckEndingService implements PipelineEnding {

    private static final Logger log = LoggerFactory.getLogger(CheckEndingService.class);

    private final TransactionTemplate transaction;
    private final JdbcSavepoints savepoints;
    private final ActiveCheckRepository activeChecks;
    private final CheckResultPort resultPort;
    private final DocumentPort documents;

    public CheckEndingService(PlatformTransactionManager transactionManager,
                              JdbcSavepoints savepoints,
                              ActiveCheckRepository activeChecks,
                              CheckResultPort resultPort,
                              DocumentPort documents) {
        TransactionTemplate template = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
        template.setName("CHK ending");
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction = template;
        this.savepoints = Objects.requireNonNull(savepoints, "savepoints");
        this.activeChecks = Objects.requireNonNull(activeChecks, "activeChecks");
        this.resultPort = Objects.requireNonNull(resultPort, "resultPort");
        this.documents = Objects.requireNonNull(documents, "documents");
    }

    /** Ends the Check COMPLETED with its report, unless another path ended it first. */
    @Override
    public void complete(Long checkId, CheckReport report) {
        Objects.requireNonNull(report, "report");
        end(checkId, () -> handOverReport(checkId, report));
    }

    /** Ends the Check FAILED with one reason and a detail, unless another path ended it first. */
    @Override
    public void fail(Long checkId, CheckFailureReason reason, String detail) {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");
        end(checkId, () -> {
            resultPort.failCheck(checkId, reason.storedValue(), detail, OffsetDateTime.now());
            log.info("CHK check ended checkId={} status=FAILED reason={}", checkId, reason.storedValue());
        });
    }

    private void end(Long checkId, Runnable handOver) {
        Objects.requireNonNull(checkId, "checkId");
        Boolean ended = transaction.execute(status -> {
            // 1 — the DELETE is the guard (REQ-CHK-078, REQ-CHK-079)
            if (activeChecks.deleteByCheckId(checkId) == 0) {
                log.debug("CHK ending skipped checkId={}: the Check has already ended", checkId);
                return Boolean.FALSE;
            }
            // 2 — the result port
            handOver.run();
            return Boolean.TRUE;
        });
        // 3 — after commit: the end-of-Check notice (REQ-CHK-061, REQ-CHK-062)
        if (Boolean.TRUE.equals(ended)) {
            documents.endCheck(checkId);
        }
    }

    private void handOverReport(Long checkId, CheckReport report) {
        try {
            savepoints.run(() -> resultPort.completeCheck(checkId,
                    report.overallStatus().storedValue(),
                    report.portFindings(),
                    report.portDocumentOutcomes(),
                    report.portUnreadQueries(),
                    report.metadata()));
            log.info("CHK check ended checkId={} status=COMPLETED overallStatus={}",
                    checkId, report.overallStatus().storedValue());
        } catch (RuntimeException notStored) {
            // REQ-CHK-048 — the report was not stored (rolled back to the savepoint, context cleared):
            // FAILED / INTERNAL_ERROR in the same transaction. If the transaction is already
            // rollback-only, its commit fails and CheckPipeline's REQUIRES_NEW fallback ends it (X1).
            log.error("CHK report of Check {} was not stored by the result port; failing it INTERNAL_ERROR",
                    checkId, notStored);
            resultPort.failCheck(checkId, CheckFailureReason.INTERNAL_ERROR.storedValue(),
                    CheckEngineTexts.english(CheckEngineErrorCodes.UNEXPECTED_FAILURE), OffsetDateTime.now());
        }
    }
}
