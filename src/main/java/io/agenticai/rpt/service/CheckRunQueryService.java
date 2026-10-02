package io.agenticai.rpt.service;

import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.repository.CheckRunRepository;
import io.agenticai.rpt.repository.UnfinishedCheckRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The reads of the Check result port (SVC-API service layer): {@code getCheck} (REQ-RPT-021) and
 * {@code listUnfinishedChecks} (REQ-RPT-022). READ_ONLY, joining the caller's transaction when there
 * is one. Module-internal: reached through {@code CheckResultStore}.
 */
@Service
public class CheckRunQueryService {

    private static final Logger log = LoggerFactory.getLogger(CheckRunQueryService.class);

    private static final EnumSet<CheckStatus> UNFINISHED =
            EnumSet.of(CheckStatus.AWAITING_DOCUMENTS, CheckStatus.RUNNING);

    private final CheckRunRepository checkRuns;

    public CheckRunQueryService(CheckRunRepository checkRuns) {
        this.checkRuns = Objects.requireNonNull(checkRuns, "checkRuns");
    }

    /**
     * {@code getCheck} — QR-RPT-001 projected to {checkId, status, serviceCode, versionNumber,
     * fetchMode, requestNumber, employeeId, startedAt}. The plan's "none → RPT-404-CHECK-NOT-FOUND"
     * is, as the Check Engine's port declares it (CON-CHK-010), an empty answer.
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public Optional<CheckRunSnapshot> findCheck(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        log.debug("RPT read check run checkId={}", checkId);
        return checkRuns.findById(checkId).map(run -> new CheckRunSnapshot(run.getCheckRunId(),
                run.getCheckStatus(), run.getServiceCode(), run.getVersionNumber(), run.getFetchMode(),
                run.getRequestNumber(), run.getEmployeeId(), run.getStartedAt()));
    }

    /**
     * {@code listUnfinishedChecks} — the Checks AWAITING_DOCUMENTS or RUNNING, ordered by STARTED_AT;
     * an empty list when there is none.
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<UnfinishedCheckRow> listUnfinishedChecks() {
        List<UnfinishedCheckRow> unfinished = checkRuns.findUnfinished(UNFINISHED);
        log.debug("RPT read unfinished check runs count={}", unfinished.size());
        return List.copyOf(unfinished);
    }
}
