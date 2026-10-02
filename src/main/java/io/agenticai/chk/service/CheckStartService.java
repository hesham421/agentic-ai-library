package io.agenticai.chk.service;

import io.agenticai.chk.contract.CheckResultPort;
import io.agenticai.chk.contract.ServiceNotAvailableException;
import io.agenticai.chk.contract.StartIncompleteException;
import io.agenticai.chk.domain.ActiveCheckStatus;
import io.agenticai.chk.entity.ActiveCheck;
import io.agenticai.chk.error.CheckEngineErrorCodes;
import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.PackageLookup;
import io.agenticai.chk.port.ServiceAvailability;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.repository.ActiveCheckRepository;
import io.agenticai.platform.config.CheckLimitsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * CON-CHK-004 — the start of a Check (SVC-API startCheck, steps 1 → 6). Honours: CON-CHK-004.
 * ONE READ_WRITE transaction: the rule checks (steps 1–3, the registry reads included) run before
 * any write (E.5.5), so a refusal creates nothing; then the result port's Check run (step 4) and
 * the Active Check (step 5) are written together. Step 6 — submitting the pipeline — is done by
 * {@link CheckEngineService} after this transaction has committed, so a pipeline never runs for a
 * Check whose start was rolled back. The transaction makes no model, MCP or host call.
 */
@Service
public class CheckStartService {

    private static final Logger log = LoggerFactory.getLogger(CheckStartService.class);

    /** RULE-CHK-008's internal log message key. */
    private static final String RULE_008 = "CHK-RULE-008";

    private final ServiceAvailability availability;
    private final PackageLookup packages;
    private final CheckResultPort resultPort;
    private final ActiveCheckRepository activeChecks;
    private final CheckLimitsProperties limits;

    public CheckStartService(ServiceAvailability availability,
                             PackageLookup packages,
                             CheckResultPort resultPort,
                             ActiveCheckRepository activeChecks,
                             CheckLimitsProperties limits) {
        this.availability = Objects.requireNonNull(availability, "availability");
        this.packages = Objects.requireNonNull(packages, "packages");
        this.resultPort = Objects.requireNonNull(resultPort, "resultPort");
        this.activeChecks = Objects.requireNonNull(activeChecks, "activeChecks");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /**
     * Steps 1 → 5, then commit.
     *
     * @throws StartIncompleteException step 1
     * @throws ServiceNotAvailableException step 2 (or step 3, when the registry reports it)
     * @throws io.agenticai.chk.contract.ConnectionNotActivatedException step 3
     */
    @Transactional
    public StartedRun start(String serviceCode, String requestNumber, String employeeId) {
        // 1 — REQ-CHK-004
        if (isBlank(serviceCode) || isBlank(requestNumber) || isBlank(employeeId)) {
            throw new StartIncompleteException();
        }
        // 2 — RULE-CHK-001 (REQ-CHK-005)
        if (!availability.isServiceAvailable(serviceCode)) {
            throw new ServiceNotAvailableException(serviceCode);
        }
        // 3 — the current version (REQ-CHK-006, REQ-CHK-007); kept in the Check's context (RULE-CHK-007)
        ServicePackageSnapshot servicePackage = packages.currentPackage(serviceCode);
        // 4 — the Check run (REQ-CHK-001, REQ-CHK-002, REQ-CHK-003, REQ-CHK-056)
        ActiveCheckStatus status = servicePackage.isManual()
                ? ActiveCheckStatus.AWAITING_DOCUMENTS : ActiveCheckStatus.RUNNING;
        OffsetDateTime startedAt = OffsetDateTime.now();
        Long checkId = resultPort.createCheckRun(servicePackage.serviceCode(), servicePackage.versionNumber(),
                servicePackage.fetchMode(), requestNumber, employeeId, status.storedValue(), startedAt);
        // 5 — the Active Check (REQ-CHK-076; RULE-CHK-008, RULE-CHK-009)
        OffsetDateTime deadlineAt = startedAt.plus(
                status == ActiveCheckStatus.AWAITING_DOCUMENTS ? limits.uploadWindow() : limits.timeout());
        try {
            activeChecks.saveAndFlush(ActiveCheck.started(checkId, status, deadlineAt));
        } catch (DataIntegrityViolationException duplicate) {
            // RULE-CHK-008 — UQ_CHK_ACTIVE_CHECK_CHECK_ID: the start is rolled back, no pipeline runs
            log.warn("{}", CheckEngineTexts.english(RULE_008, checkId));
            throw new CheckEngineException(CheckEngineErrorCodes.UNEXPECTED_FAILURE, duplicate);
        }
        log.info("CHK check started checkId={} serviceCode={} versionNumber={} status={}",
                checkId, servicePackage.serviceCode(), servicePackage.versionNumber(), status.storedValue());
        // 6 — commit; the facade submits a RUNNING Check's pipeline after the commit
        CheckContext context = status == ActiveCheckStatus.RUNNING
                ? CheckContext.of(checkId, servicePackage, requestNumber, employeeId, startedAt, deadlineAt.toInstant())
                : null;
        return new StartedRun(checkId, status, context);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
