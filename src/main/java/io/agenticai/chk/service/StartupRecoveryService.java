package io.agenticai.chk.service;

import io.agenticai.chk.contract.CheckResultPort;
import io.agenticai.chk.contract.UnfinishedCheck;
import io.agenticai.chk.domain.CheckFailureReason;
import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.DocumentPort;
import io.agenticai.chk.repository.ActiveCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * {@code StartupRecoveryService.onApplicationReady()} — the start-up recovery (REQ-CHK-055,
 * REQ-CHK-081; ADR-CHK-005, ADR-CHK-015), run once per start, single-threaded:
 * <ol>
 *   <li>the result port's unfinished Checks → each {@code failCheck(INTERRUPTED, "interrupted by a
 *       restart of the service", now)} in its own short transaction, then Document Access's
 *       end-of-Check notice after that commit;</li>
 *   <li>{@code DELETE FROM CHK_ACTIVE_CHECK} — every Active Check of the earlier run.</li>
 * </ol>
 *
 * <p><b>Ordering.</b> A {@link SmartLifecycle} at {@link #PHASE} 0: every bean is wired before it
 * starts; the embedded web server (phase {@code Integer.MAX_VALUE - 1}) starts after it, so INT's
 * endpoints — the only callers of {@code CheckEngine} — accept no request before recovery is done;
 * and {@code @Scheduled} tasks are registered only once the context is refreshed, after every
 * lifecycle bean has started, so the deadline check never runs before it. REG's load run is also at
 * phase 0 and their relative order is not fixed: recovery reads nothing of the registry (only the
 * result port, Document Access and CHK's own table), so it does not need REG to be loaded.
 */
@Component
public class StartupRecoveryService implements SmartLifecycle {

    /** Before the web server; independent of REG's load run (see the class comment). */
    public static final int PHASE = 0;

    private static final Logger log = LoggerFactory.getLogger(StartupRecoveryService.class);

    /** The plan's detail text of step 1. */
    private static final String INTERRUPTED_DETAIL = "CHK-DETAIL-INTERRUPTED";

    private final TransactionTemplate transaction;
    private final CheckResultPort resultPort;
    private final DocumentPort documents;
    private final ActiveCheckRepository activeChecks;

    private volatile boolean running;

    public StartupRecoveryService(PlatformTransactionManager transactionManager,
                                  CheckResultPort resultPort,
                                  DocumentPort documents,
                                  ActiveCheckRepository activeChecks) {
        TransactionTemplate template = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
        template.setName("CHK start-up recovery");
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction = template;
        this.resultPort = Objects.requireNonNull(resultPort, "resultPort");
        this.documents = Objects.requireNonNull(documents, "documents");
        this.activeChecks = Objects.requireNonNull(activeChecks, "activeChecks");
    }

    /** Steps 1 → 2. */
    public void onApplicationReady() {
        // 1 — end every unfinished Check INTERRUPTED (REQ-CHK-055)
        List<UnfinishedCheck> unfinished = resultPort.listUnfinishedChecks();
        String detail = CheckEngineTexts.english(INTERRUPTED_DETAIL);
        for (UnfinishedCheck check : unfinished) {
            transaction.executeWithoutResult(status -> resultPort.failCheck(check.checkId(),
                    CheckFailureReason.INTERRUPTED.storedValue(), detail, OffsetDateTime.now()));
            documents.endCheck(check.checkId());
        }
        // 2 — every Active Check of the earlier run (REQ-CHK-081)
        Integer deleted = transaction.execute(status -> activeChecks.deleteEveryActiveCheck());
        log.info("CHK start-up recovery: {} unfinished Check(s) ended INTERRUPTED, {} Active Check(s) deleted",
                unfinished.size(), deleted);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        onApplicationReady();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
