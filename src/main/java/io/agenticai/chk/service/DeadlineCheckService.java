package io.agenticai.chk.service;

import io.agenticai.chk.domain.ActiveCheckStatus;
import io.agenticai.chk.domain.CheckFailureReason;
import io.agenticai.chk.repository.ActiveCheckRepository;
import io.agenticai.chk.repository.ActiveCheckRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * {@code DeadlineCheckService.run()} — the deadline check (REQ-CHK-051, REQ-CHK-060, REQ-CHK-080):
 * every {@code aias.check.deadline-check-interval} (fixed delay; the platform default when the key
 * is unset), a READ_ONLY read of the Active Checks whose deadline has passed, then one ending per
 * row, each in the ending's own transaction:
 * <ul>
 *   <li>AWAITING_DOCUMENTS → FAILED / UPLOAD_WINDOW_EXPIRED (REQ-CHK-060);</li>
 *   <li>RUNNING → the Check ends FAILED / TIMED_OUT (REQ-CHK-051) and only THEN is the pipeline's
 *       {@code Future} cancelled (interrupt); a model answer arriving later finds the Active Check
 *       gone and is discarded (REQ-CHK-079).</li>
 * </ul>
 *
 * <p><b>Order for a RUNNING row — end first, cancel after.</b> SVC-API's prose reads "cancel the
 * pipeline's {@code Future} (interrupt) and {@code fail(TIMED_OUT)}". Cancelling first lets the
 * interrupted pipeline reach its own ending first: an interrupt inside a Document Access, result
 * port or JDBC call surfaces as an exception that maps to INTERNAL_ERROR, and that ending could
 * win the DELETE, so a timed-out Check would not end TIMED_OUT as REQ-CHK-051 requires. The
 * ending's DELETE is the guard (REQ-CHK-079, ADR-CHK-015): once this path has deleted the row and
 * committed, the interrupted pipeline's own ending deletes 0 rows and stops. Hence the ending is
 * called first and the pipeline cancelled after it (ALIGN-BE-CHK F3, orchestrator decision).
 * The ending's DELETE decides a race with any other ending path. Scheduling is enabled by
 * {@code chk.config.CheckSchedulingConfiguration}; the first run happens after the start-up
 * recovery ({@link StartupRecoveryService}) — scheduled tasks start once the context is refreshed,
 * after every lifecycle bean has started.
 */
@Service
public class DeadlineCheckService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineCheckService.class);

    private final ActiveCheckRepository activeChecks;
    private final PipelineRunner runner;
    private final CheckEndingService ending;

    public DeadlineCheckService(ActiveCheckRepository activeChecks, PipelineRunner runner, CheckEndingService ending) {
        this.activeChecks = Objects.requireNonNull(activeChecks, "activeChecks");
        this.runner = Objects.requireNonNull(runner, "runner");
        this.ending = Objects.requireNonNull(ending, "ending");
    }

    @Scheduled(fixedDelayString = "${aias.check.deadline-check-interval:"
            + "#{T(io.agenticai.platform.config.CheckLimitsProperties).DEFAULT_DEADLINE_CHECK_INTERVAL.toString()}}")
    public void run() {
        List<ActiveCheckRow> due = activeChecks.findDue(OffsetDateTime.now());
        log.debug("CHK deadline check: {} Active Check(s) past their deadline", due.size());
        for (ActiveCheckRow row : due) {
            try {
                endOne(row);
            } catch (RuntimeException failure) {
                // the row stays; the next run ends it — one failing Check never stops the others
                log.error("CHK deadline check could not end Check {}", row.checkId(), failure);
            }
        }
    }

    private void endOne(ActiveCheckRow row) {
        if (row.checkStatus() == ActiveCheckStatus.AWAITING_DOCUMENTS) {
            ending.fail(row.checkId(), CheckFailureReason.UPLOAD_WINDOW_EXPIRED,
                    CheckFailureReason.UPLOAD_WINDOW_EXPIRED.storedValue());
            return;
        }
        // end first — the DELETE decides the race — then stop the pipeline (REQ-CHK-051, REQ-CHK-079)
        PipelineFailure timedOut = PipelineFailure.timedOut();
        ending.fail(row.checkId(), timedOut.reason(), timedOut.detail());
        runner.cancel(row.checkId());
    }
}
