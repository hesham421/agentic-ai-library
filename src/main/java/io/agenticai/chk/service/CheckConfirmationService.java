package io.agenticai.chk.service;

import io.agenticai.chk.contract.CheckNotAwaitingDocumentsException;
import io.agenticai.chk.contract.CheckNotFoundException;
import io.agenticai.chk.contract.CheckResultPort;
import io.agenticai.chk.contract.CheckRun;
import io.agenticai.chk.domain.ActiveCheckStatus;
import io.agenticai.chk.domain.CheckFailureReason;
import io.agenticai.chk.entity.ActiveCheck;
import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.PackageLookup;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.repository.ActiveCheckRepository;
import io.agenticai.platform.config.CheckLimitsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * CON-CHK-005 — the confirmation of a {@code manual} Check's uploads (SVC-API confirmUploads,
 * steps 1 → 4). Honours: CON-CHK-005.
 *
 * <p>{@link #confirm} is steps 1 → 3 in ONE READ_WRITE transaction: the locking read
 * ({@code FOR UPDATE}) serialises the confirmation against the deadline check — whichever commits
 * first wins; RULE-CHK-010 is decided under the lock before any write. {@link #resume} is step 4,
 * after the commit and outside any transaction: the Check's recorded version is resolved
 * (RULE-CHK-007) and the pipeline's working data built; a version that cannot be resolved ends the
 * Check FAILED / INTERNAL_ERROR with RULE-CHK-007's message.
 */
@Service
public class CheckConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(CheckConfirmationService.class);

    /** RULE-CHK-007's message key. */
    private static final String RULE_007 = "CHK-RULE-007";

    private final ActiveCheckRepository activeChecks;
    private final CheckResultPort resultPort;
    private final PackageLookup packages;
    private final CheckEndingService ending;
    private final CheckLimitsProperties limits;

    public CheckConfirmationService(ActiveCheckRepository activeChecks,
                                    CheckResultPort resultPort,
                                    PackageLookup packages,
                                    CheckEndingService ending,
                                    CheckLimitsProperties limits) {
        this.activeChecks = Objects.requireNonNull(activeChecks, "activeChecks");
        this.resultPort = Objects.requireNonNull(resultPort, "resultPort");
        this.packages = Objects.requireNonNull(packages, "packages");
        this.ending = Objects.requireNonNull(ending, "ending");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Steps 1 → 3, then commit.
     *
     * @throws CheckNotFoundException             step 2 — the result port knows no such Check (REQ-CHK-058)
     * @throws CheckNotAwaitingDocumentsException step 2 — RULE-CHK-010 (REQ-CHK-059)
     */
    @Transactional
    public ConfirmedRun confirm(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        // 1 — the locking read
        Optional<ActiveCheck> active = activeChecks.findForUpdateByCheckId(checkId);
        // 2 — RULE-CHK-010
        if (active.isEmpty()) {
            CheckRun run = resultPort.getCheck(checkId).orElseThrow(() -> new CheckNotFoundException(checkId));
            throw new CheckNotAwaitingDocumentsException(checkId, run.status());
        }
        ActiveCheckStatus status = active.get().getCheckStatus();
        if (status != ActiveCheckStatus.AWAITING_DOCUMENTS) {
            throw new CheckNotAwaitingDocumentsException(checkId, status.storedValue());
        }
        // 3 — RUNNING, the timeout counted from now (REQ-CHK-052, REQ-CHK-077, REQ-CHK-057)
        OffsetDateTime runningSince = OffsetDateTime.now();
        OffsetDateTime deadlineAt = runningSince.plus(limits.timeout());
        activeChecks.confirmRunning(checkId, ActiveCheckStatus.RUNNING.storedValue(), deadlineAt);
        resultPort.markRunning(checkId, runningSince);
        log.info("CHK uploads confirmed checkId={} status=RUNNING", checkId);
        return new ConfirmedRun(checkId, deadlineAt);
    }

    /**
     * Step 4, after the commit: the recorded version (RULE-CHK-007, REQ-CHK-008).
     *
     * @return the pipeline's working data; empty when the Check was ended FAILED instead
     */
    public Optional<CheckContext> resume(ConfirmedRun confirmed) {
        Long checkId = confirmed.checkId();
        Optional<CheckRun> recorded = resultPort.getCheck(checkId);
        if (recorded.isEmpty()) {
            PipelineFailure failure = PipelineFailure.unexpected();
            ending.fail(checkId, failure.reason(), failure.detail());
            return Optional.empty();
        }
        CheckRun run = recorded.get();
        Optional<ServicePackageSnapshot> pinned = packages.pinnedPackage(run.serviceCode(), run.versionNumber());
        if (pinned.isEmpty()) {
            log.warn("CHK recorded version not resolved checkId={} serviceCode={} versionNumber={}",
                    checkId, run.serviceCode(), run.versionNumber());
            ending.fail(checkId, CheckFailureReason.INTERNAL_ERROR,
                    CheckEngineTexts.english(RULE_007, checkId, run.versionNumber(), run.serviceCode()));
            return Optional.empty();
        }
        return Optional.of(CheckContext.of(checkId, pinned.get(), run.requestNumber(), run.employeeId(),
                run.startedAt(), confirmed.deadlineAt().toInstant()));
    }
}
