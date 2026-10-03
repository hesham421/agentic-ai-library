package io.agenticai.rpt.service;

import io.agenticai.rpt.config.ReportStoreProperties;
import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.error.ReportStoreTexts;
import io.agenticai.rpt.repository.CheckRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The scheduled purge of ended Check runs (SVC-API {@code ReportPurgeService}; ADR-RPT-004,
 * ADR-RPT-010; REQ-RPT-042 … REQ-RPT-046, REQ-RPT-052, REQ-RPT-054). No HTTP API, no caller. Runs
 * on {@code aias.reports.purge-schedule} (default daily 02:00) through the deployable's one
 * {@code @EnableScheduling} — CHK's {@code CheckSchedulingConfiguration}; RPT adds none.
 *
 * <ol>
 *   <li>No valid retention period → logs the skip line and deletes nothing (REQ-RPT-044).</li>
 *   <li>cutOff = now − retention days; reads the ids of the COMPLETED / FAILED runs ended before
 *       it — never an AWAITING_DOCUMENTS or RUNNING run, whatever its age (REQ-RPT-045).</li>
 *   <li>Each id in its own transaction ({@code REQUIRES_NEW}): a hard DELETE re-checking status and
 *       age, the database cascading its Findings, Check Documents and Unread Queries
 *       (REQ-RPT-043). A failure rolls back that run only, which stays whole (REQ-RPT-052); it is
 *       logged at WARN with the identifier and the database's message, never row content
 *       (REQ-RPT-054), and is not counted. A run another instance deleted first counts nothing.</li>
 *   <li>Logs the number deleted and the cut-off (REQ-RPT-046).</li>
 * </ol>
 */
@Service
public class ReportPurgeService {

    private static final Logger log = LoggerFactory.getLogger(ReportPurgeService.class);

    /** Message key of the kept-run WARN line (REQ-RPT-054, ADR-RPT-020) — not an error code. */
    private static final String PURGE_KEPT = "RPT-LOG-PURGE-KEPT";

    /** Message key of the closing count line (REQ-RPT-046) — not an error code. */
    private static final String PURGE_DELETED = "RPT-LOG-PURGE-DELETED";

    private static final EnumSet<CheckStatus> ENDED = EnumSet.of(CheckStatus.COMPLETED, CheckStatus.FAILED);

    private final ReportStoreProperties properties;
    private final CheckRunRepository checkRuns;
    private final TransactionTemplate oneRunTransaction;

    public ReportPurgeService(ReportStoreProperties properties,
                              CheckRunRepository checkRuns,
                              PlatformTransactionManager transactionManager) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.checkRuns = Objects.requireNonNull(checkRuns, "checkRuns");
        TransactionTemplate template = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
        template.setName("RPT purge of one Check run");
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.oneRunTransaction = template;
    }

    /** One purge run; scheduled, never called over HTTP. */
    @Scheduled(cron = "${aias.reports.purge-schedule:0 0 2 * * *}")
    public void purge() {
        // 1 — no valid retention period: nothing is deleted
        if (!properties.hasValidRetentionPeriod()) {
            log.info(ReportStoreTexts.english(ReportStoreTexts.PURGE_SKIPPED));
            return;
        }

        // 2 — the ended runs older than the period
        OffsetDateTime cutOff = OffsetDateTime.now().minusDays(properties.retentionDays());
        List<Long> expired = checkRuns.findEndedBefore(ENDED, cutOff);

        // 3 — one transaction per Check run
        int deleted = 0;
        for (Long checkId : expired) {
            // the deletion's own failure, kept apart: when the rollback fails as well, the exception that leaves
            // the template is the rollback's, and the database's message of the deletion would be lost (REQ-RPT-054)
            AtomicReference<DataAccessException> deletionFailure = new AtomicReference<>();
            try {
                Integer rows = oneRunTransaction.execute(status -> {
                    try {
                        return checkRuns.deleteEnded(checkId, cutOff);
                    } catch (DataAccessException deletionFailed) {
                        deletionFailure.set(deletionFailed);
                        throw deletionFailed;
                    }
                });
                if (rows != null && rows > 0) {
                    deleted++;
                }
            } catch (DataAccessException | TransactionException failed) {
                RuntimeException cause = deletionFailure.get() != null ? deletionFailure.get() : failed;
                log.warn(ReportStoreTexts.english(PURGE_KEPT, checkId,
                        NestedExceptionUtils.getMostSpecificCause(cause).getMessage()));
            }
        }

        // 4 — the count and the cut-off
        log.info(ReportStoreTexts.english(PURGE_DELETED, deleted, cutOff));
    }
}
