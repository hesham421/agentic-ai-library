package io.agenticai.integration.service;

import io.agenticai.integration.domain.ApprovalDefinition;
import io.agenticai.integration.domain.ApprovalGuard;
import io.agenticai.integration.domain.CheckSnapshot;
import io.agenticai.integration.domain.DecisionCommand;
import io.agenticai.integration.dto.DecisionRequest;
import io.agenticai.integration.dto.RecordedDecisionResponse;
import io.agenticai.integration.error.IntegrationTexts;
import io.agenticai.integration.port.ApprovalDefinitionPort;
import io.agenticai.integration.port.CheckRecordPort;
import io.agenticai.integration.port.HostApprovalPort;
import io.agenticai.rpt.contract.RecordedDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * API-INT-004 — Record an Employee Decision (CON-INT-004). The ONLY class that receives
 * {@link HostApprovalPort} and {@link ApprovalDefinitionPort} (REQ-INT-029, AIAS-4): the host
 * Approval API is called from {@link #decide} step 3 and from nowhere else.
 *
 * <p>Steps, exactly as SVC-API-COMMAND orders them:
 * <ol>
 *   <li>parse ({@code INT-400-REQUEST-INVALID}, by the advice); load the Check through
 *       {@link CheckRecordPort#read} (unknown → {@code RPT-404-CHECK-NOT-FOUND}, passed through).</li>
 *   <li>When the decision is {@code APPROVED}: the approval definition of the Check's OWN version
 *       — service code DBF-INT-003, version DBF-INT-004 (REQ-INT-030).</li>
 *   <li>Approval path — {@code APPROVED} and the version enables the Approval API (REQ-INT-025):
 *       take the per-Check lock; re-read the Check; RULE-INT-002 / RULE-INT-003 through
 *       {@link ApprovalGuard} (the Report Store's own codes — ADR-INT-010), so every refusal
 *       happens before the host receives a call (REQ-INT-034, REQ-INT-035); then
 *       {@link HostApprovalPort#approve} exactly once (REQ-INT-031 … REQ-INT-033). Its failure
 *       ({@code INT-502-APPROVAL-API-FAILED}) or timeout ({@code INT-504-APPROVAL-API-TIMED-OUT})
 *       propagates and nothing is recorded (REQ-INT-036 … REQ-INT-038). On success the decision is
 *       handed to the Report Store with {@code approvalApiExecuted = true} (REQ-INT-026); a refusal
 *       of that hand-over is answered unchanged and logged at WARN with the Check identifier and
 *       request number (REQ-INT-039). The lock is released in {@code finally}.</li>
 *   <li>Every other case — {@code REJECTED} (REQ-INT-027), a version that does not enable the
 *       Approval API (REQ-INT-028), or a code that is not {@code APPROVED}: no Approval API call;
 *       the decision is handed to the Report Store with {@code approvalApiExecuted = false}
 *       (REQ-INT-021), whose refusals pass through unchanged (REQ-INT-006).</li>
 *   <li>Answer the recorded decision (REQ-INT-022).</li>
 * </ol>
 *
 * <p><b>Concurrency.</b> A per-Check-identifier in-process lock, held across step 3 from the
 * re-read to the hand-over: two simultaneous APPROVED decisions on one approval-enabled Check
 * cannot both call the host — the second waits, re-reads, finds the decision and is refused
 * {@code RPT-409-DECISION-ALREADY-RECORDED} before any call. The map holds Check identifiers and
 * lock objects only — never a request, a decision or a report (REQ-INT-059, G9); an entry is
 * removed when its last holder or waiter releases it, so the map is empty between requests. The
 * Report Store's conditional update stays the final guard for every path. A multi-instance
 * deployment is not covered (ADR-INT-017 (6)).
 *
 * <p><b>No {@code @Transactional}</b>, deliberately: INT owns no table, and no database
 * transaction may be held open across the host Approval API call (A.5.5); the Report Store's
 * reads and its recording of the decision run in its own transactions.
 */
@Service("intDecisionService")
public class DecisionService {

    private static final Logger log = LoggerFactory.getLogger(DecisionService.class);

    /** The Report Store's EMPLOYEE_DECISION code that may be executed through the Approval API (CON-RPT-002). */
    private static final String APPROVED = "APPROVED";

    /** The WARN text of REQ-INT-039 (messages.properties). */
    private static final String LOG_APPROVAL_NOT_RECORDED = "INT-LOG-APPROVAL-NOT-RECORDED";

    private final CheckRecordPort checkRecords;
    private final ApprovalDefinitionPort approvalDefinitions;
    private final HostApprovalPort hostApproval;

    /** Per-Check approval locks: Check identifier → lock with its holder count. Ids only (G9). */
    private final ConcurrentHashMap<Long, CheckLock> approvalLocks = new ConcurrentHashMap<>();

    public DecisionService(CheckRecordPort checkRecords,
                           ApprovalDefinitionPort approvalDefinitions,
                           HostApprovalPort hostApproval) {
        this.checkRecords = Objects.requireNonNull(checkRecords, "checkRecords");
        this.approvalDefinitions = Objects.requireNonNull(approvalDefinitions, "approvalDefinitions");
        this.hostApproval = Objects.requireNonNull(hostApproval, "hostApproval");
    }

    /**
     * Records the Employee Decision on the Check {@code checkId}, through the host Approval API where
     * the decision is APPROVED and the Check's version enables it.
     *
     * @param checkId DBF-INT-001
     * @param request the decision exactly as received
     * @return the recorded decision
     */
    public RecordedDecisionResponse decide(Long checkId, DecisionRequest request) {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(request, "request");
        DecisionCommand decision = request.toCommand(checkId);

        // 1. load the Check
        CheckSnapshot check = checkRecords.read(checkId);

        // 2. the approval definition of the Check's own version — only for an APPROVED decision
        // 3. the approval path
        if (APPROVED.equals(decision.employeeDecision())) {
            ApprovalDefinition definition =
                    approvalDefinitions.definitionOf(check.serviceCode(), check.versionNumber());
            if (definition.enabled()) {
                return RecordedDecisionResponse.of(approve(decision, definition));
            }
        }

        // 4. every other case: no Approval API call
        RecordedDecision recorded = checkRecords.handOverDecision(
                checkId, decision.employeeDecision(), decision.decidedBy(), false);
        log.info("INT decision recorded checkId={} employeeDecision={} approvalApiExecuted=false",
                checkId, recorded.employeeDecision());
        // 5.
        return RecordedDecisionResponse.of(recorded);
    }

    /** Step 3 — under the per-Check lock: re-read, RULE-INT-002/003, one Approval API call, hand-over. */
    private RecordedDecision approve(DecisionCommand decision, ApprovalDefinition definition) {
        Long checkId = decision.checkId();
        CheckLock lock = lock(checkId);
        try {
            CheckSnapshot current = checkRecords.read(checkId);
            ApprovalGuard.forDecision(current, decision).requireApprovalAllowed();

            hostApproval.approve(definition, current.requestNumber(), checkId, decision.decidedBy());
            log.info("INT approval executed checkId={}", checkId);

            RecordedDecision recorded;
            try {
                recorded = checkRecords.handOverDecision(
                        checkId, decision.employeeDecision(), decision.decidedBy(), true);
            } catch (RuntimeException notRecorded) {
                log.warn(IntegrationTexts.english(LOG_APPROVAL_NOT_RECORDED, checkId, current.requestNumber()));
                throw notRecorded;
            }
            log.info("INT decision recorded checkId={} employeeDecision={} approvalApiExecuted=true",
                    checkId, recorded.employeeDecision());
            return recorded;
        } finally {
            unlock(checkId, lock);
        }
    }

    /** Takes the lock of {@code checkId}, registering this holder so the entry outlives every waiter. */
    private CheckLock lock(Long checkId) {
        CheckLock lock = approvalLocks.compute(checkId, (id, existing) -> {
            CheckLock entry = existing == null ? new CheckLock() : existing;
            entry.holders++;
            return entry;
        });
        lock.lock.lock();
        return lock;
    }

    /** Releases the lock of {@code checkId}; the entry is removed when no holder or waiter remains. */
    private void unlock(Long checkId, CheckLock lock) {
        lock.lock.unlock();
        approvalLocks.computeIfPresent(checkId, (id, entry) -> --entry.holders == 0 ? null : entry);
    }

    /**
     * One Check's approval lock and the number of requests holding or waiting for it; the count is
     * changed only inside {@link ConcurrentHashMap#compute} / {@code computeIfPresent} of its key.
     */
    private static final class CheckLock {
        private final ReentrantLock lock = new ReentrantLock();
        private int holders;
    }
}
