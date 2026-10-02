package io.agenticai.rpt.repository;

import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.entity.CheckRun;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of ENT-RPT-001 Check Run ({@code RPT_CHECK_RUN}). Module-internal.
 *
 * <p>Exactly the operations SVC-API names: QR-RPT-001 (inherited {@code findById}, READ_ONLY;
 * {@link #findForUpdate} for {@code completeCheck}), QR-RPT-005, QR-RPT-006, QR-RPT-007, and the
 * in-process operations the plan writes inline — the {@code createCheckRun} INSERT (inherited
 * {@code saveAndFlush}), the conditional UPDATEs of {@code markRunning}, {@code completeCheck},
 * {@code failCheck} and {@code recordDecision}, the {@code listUnfinishedChecks} read and the
 * purge's read and DELETE. Every UPDATE sets {@code UPDATED_AT = SYSTIMESTAMP}; it is native
 * because {@code UPDATED_AT} is mapped read-only on the entity and the plan prescribes the
 * database's own {@code SYSTIMESTAMP} (oracle19c). All values are bound parameters (REQ-RPT-050);
 * the status literals are the plan's.
 */
public interface CheckRunRepository extends JpaRepository<CheckRun, Long> {

    /**
     * QR-RPT-001 with {@code FOR UPDATE} — {@code completeCheck} step 1: the locking read that
     * serialises a completion against a concurrent fail. Runs in the caller's READ_WRITE
     * transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CheckRun c WHERE c.checkRunId = :checkId")
    Optional<CheckRun> findForUpdate(@Param("checkId") Long checkId);

    /**
     * {@code markRunning} step 1 — {@code UPDATE RPT_CHECK_RUN SET CHECK_STATUS = 'RUNNING',
     * RUNNING_SINCE = COALESCE(RUNNING_SINCE, :runningSince), UPDATED_AT = SYSTIMESTAMP WHERE
     * CHECK_RUN_ID = :checkId AND CHECK_STATUS IN ('AWAITING_DOCUMENTS', 'RUNNING')} — the guard of
     * RULE-RPT-003.
     *
     * @return the rows updated — 0 when the Check does not exist or has ended
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE RPT_CHECK_RUN SET CHECK_STATUS = 'RUNNING', "
            + "RUNNING_SINCE = COALESCE(RUNNING_SINCE, :runningSince), UPDATED_AT = SYSTIMESTAMP "
            + "WHERE CHECK_RUN_ID = :checkId AND CHECK_STATUS IN ('AWAITING_DOCUMENTS', 'RUNNING')",
            nativeQuery = true)
    int markRunning(@Param("checkId") Long checkId, @Param("runningSince") OffsetDateTime runningSince);

    /**
     * {@code completeCheck} step 3 — {@code UPDATE RPT_CHECK_RUN SET CHECK_STATUS = 'COMPLETED',
     * OVERALL_STATUS = :overallStatus, COMPARISON_MODEL = :comparisonModel, ENDED_AT = :endedAt,
     * UPDATED_AT = SYSTIMESTAMP WHERE CHECK_RUN_ID = :checkId AND CHECK_STATUS = 'RUNNING'}.
     *
     * @param overallStatus the OVERALL_STATUS stored value
     * @return the rows updated
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE RPT_CHECK_RUN SET CHECK_STATUS = 'COMPLETED', OVERALL_STATUS = :overallStatus, "
            + "COMPARISON_MODEL = :comparisonModel, ENDED_AT = :endedAt, UPDATED_AT = SYSTIMESTAMP "
            + "WHERE CHECK_RUN_ID = :checkId AND CHECK_STATUS = 'RUNNING'",
            nativeQuery = true)
    int complete(@Param("checkId") Long checkId,
                 @Param("overallStatus") String overallStatus,
                 @Param("comparisonModel") String comparisonModel,
                 @Param("endedAt") OffsetDateTime endedAt);

    /**
     * {@code failCheck} step 2 — {@code UPDATE RPT_CHECK_RUN SET CHECK_STATUS = 'FAILED',
     * FAILURE_REASON = :failureReason, FAILURE_DETAIL = :failureDetail, ENDED_AT = :endedAt,
     * UPDATED_AT = SYSTIMESTAMP WHERE CHECK_RUN_ID = :checkId AND CHECK_STATUS IN
     * ('AWAITING_DOCUMENTS', 'RUNNING')} — the guard of RULE-RPT-003.
     *
     * @param failureReason the CHECK_FAILURE_REASON stored value
     * @return the rows updated — 0 when the Check does not exist or has ended
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE RPT_CHECK_RUN SET CHECK_STATUS = 'FAILED', FAILURE_REASON = :failureReason, "
            + "FAILURE_DETAIL = :failureDetail, ENDED_AT = :endedAt, UPDATED_AT = SYSTIMESTAMP "
            + "WHERE CHECK_RUN_ID = :checkId AND CHECK_STATUS IN ('AWAITING_DOCUMENTS', 'RUNNING')",
            nativeQuery = true)
    int fail(@Param("checkId") Long checkId,
             @Param("failureReason") String failureReason,
             @Param("failureDetail") String failureDetail,
             @Param("endedAt") OffsetDateTime endedAt);

    /**
     * {@code recordDecision} step 3 — {@code UPDATE RPT_CHECK_RUN SET EMPLOYEE_DECISION =
     * :employeeDecision, DECIDED_BY = :decidedBy, DECIDED_AT = SYSTIMESTAMP, APPROVAL_API_EXECUTED
     * = :approvalApiExecuted, UPDATED_AT = SYSTIMESTAMP WHERE CHECK_RUN_ID = :checkId AND
     * CHECK_STATUS = 'COMPLETED' AND EMPLOYEE_DECISION IS NULL} — the guard of RULE-RPT-011 and
     * RULE-RPT-012.
     *
     * @param employeeDecision    the EMPLOYEE_DECISION stored value
     * @param approvalApiExecuted {@code NUMBER(1)}: 1 executed through the Approval API, 0 not
     * @return the rows updated — 0 when the Check does not exist, is not COMPLETED or is decided
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE RPT_CHECK_RUN SET EMPLOYEE_DECISION = :employeeDecision, DECIDED_BY = :decidedBy, "
            + "DECIDED_AT = SYSTIMESTAMP, APPROVAL_API_EXECUTED = :approvalApiExecuted, UPDATED_AT = SYSTIMESTAMP "
            + "WHERE CHECK_RUN_ID = :checkId AND CHECK_STATUS = 'COMPLETED' AND EMPLOYEE_DECISION IS NULL",
            nativeQuery = true)
    int recordDecision(@Param("checkId") Long checkId,
                       @Param("employeeDecision") String employeeDecision,
                       @Param("decidedBy") String decidedBy,
                       @Param("approvalApiExecuted") int approvalApiExecuted);

    /**
     * {@code listUnfinishedChecks} — {@code SELECT CHECK_RUN_ID, CHECK_STATUS, STARTED_AT FROM
     * RPT_CHECK_RUN WHERE CHECK_STATUS IN ('AWAITING_DOCUMENTS', 'RUNNING') ORDER BY STARTED_AT}
     * (IDX_RPT_CHECK_RUN_CHECK_STATUS), READ_ONLY. The statuses are bound through the entity's
     * converter.
     */
    @Transactional(readOnly = true)
    @Query("SELECT new io.agenticai.rpt.repository.UnfinishedCheckRow(c.checkRunId, c.checkStatus, c.startedAt) "
            + "FROM CheckRun c WHERE c.checkStatus IN :statuses ORDER BY c.startedAt")
    List<UnfinishedCheckRow> findUnfinished(@Param("statuses") Collection<CheckStatus> statuses);

    /**
     * QR-RPT-005 — {@code SELECT CHECK_RUN_ID, CHECK_STATUS, OVERALL_STATUS, STARTED_AT, ENDED_AT,
     * EMPLOYEE_DECISION FROM RPT_CHECK_RUN WHERE SERVICE_CODE = :serviceCode AND REQUEST_NUMBER =
     * :requestNumber ORDER BY STARTED_AT DESC, CHECK_RUN_ID DESC FETCH FIRST 100 ROWS ONLY}
     * (IDX_RPT_CHECK_RUN_SVC_REQ), READ_ONLY. The cap is given as {@code Limit.of(LIST_LIMIT)}.
     */
    @Transactional(readOnly = true)
    @Query("SELECT new io.agenticai.rpt.repository.CheckSummaryRow(c.checkRunId, c.checkStatus, c.overallStatus, "
            + "c.startedAt, c.endedAt, c.employeeDecision) FROM CheckRun c "
            + "WHERE c.serviceCode = :serviceCode AND c.requestNumber = :requestNumber "
            + "ORDER BY c.startedAt DESC, c.checkRunId DESC")
    List<CheckSummaryRow> findNewestOfRequest(@Param("serviceCode") String serviceCode,
                                              @Param("requestNumber") String requestNumber,
                                              Limit limit);

    /**
     * QR-RPT-006 — {@code SELECT COUNT(*) FROM RPT_CHECK_RUN WHERE SERVICE_CODE = :serviceCode AND
     * REQUEST_NUMBER = :requestNumber}, READ_ONLY.
     */
    @Transactional(readOnly = true)
    @Query("SELECT COUNT(c) FROM CheckRun c WHERE c.serviceCode = :serviceCode AND c.requestNumber = :requestNumber")
    long countOfRequest(@Param("serviceCode") String serviceCode, @Param("requestNumber") String requestNumber);

    /**
     * QR-RPT-007 — {@code SELECT VERSION_NUMBER, OVERALL_STATUS, EMPLOYEE_DECISION, COUNT(*) FROM
     * RPT_CHECK_RUN WHERE SERVICE_CODE = :serviceCode AND EMPLOYEE_DECISION IS NOT NULL GROUP BY
     * VERSION_NUMBER, OVERALL_STATUS, EMPLOYEE_DECISION ORDER BY VERSION_NUMBER DESC,
     * OVERALL_STATUS, EMPLOYEE_DECISION}, READ_ONLY.
     */
    @Transactional(readOnly = true)
    @Query("SELECT new io.agenticai.rpt.repository.AgreementCountRow(c.versionNumber, c.overallStatus, "
            + "c.employeeDecision, COUNT(c)) FROM CheckRun c "
            + "WHERE c.serviceCode = :serviceCode AND c.employeeDecision IS NOT NULL "
            + "GROUP BY c.versionNumber, c.overallStatus, c.employeeDecision "
            + "ORDER BY c.versionNumber DESC, c.overallStatus, c.employeeDecision")
    List<AgreementCountRow> findDecisionAgreement(@Param("serviceCode") String serviceCode);

    /**
     * The purge's step 2 — {@code SELECT CHECK_RUN_ID FROM RPT_CHECK_RUN WHERE CHECK_STATUS IN
     * ('COMPLETED', 'FAILED') AND ENDED_AT < :cutOff} (IDX_RPT_CHECK_RUN_ENDED_AT), READ_ONLY. The
     * statuses are bound through the entity's converter.
     */
    @Transactional(readOnly = true)
    @Query("SELECT c.checkRunId FROM CheckRun c WHERE c.checkStatus IN :statuses AND c.endedAt < :cutOff")
    List<Long> findEndedBefore(@Param("statuses") Collection<CheckStatus> statuses,
                               @Param("cutOff") OffsetDateTime cutOff);

    /**
     * The purge's step 3 — {@code DELETE FROM RPT_CHECK_RUN WHERE CHECK_RUN_ID = :checkId AND
     * CHECK_STATUS IN ('COMPLETED', 'FAILED') AND ENDED_AT < :cutOff}; FK_RPT_FINDING_RPT_CHECK_RUN,
     * FK_RPT_CHECK_DOCUMENT_RPT_CHECK_RUN and FK_RPT_UNREAD_QUERY_RPT_CHECK_RUN ({@code ON DELETE
     * CASCADE}) remove the Findings, Check Documents and Unread Queries (REQ-RPT-043). Hard delete.
     *
     * @return the rows deleted — 0 when another purge deleted it first
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM RPT_CHECK_RUN WHERE CHECK_RUN_ID = :checkId "
            + "AND CHECK_STATUS IN ('COMPLETED', 'FAILED') AND ENDED_AT < :cutOff",
            nativeQuery = true)
    int deleteEnded(@Param("checkId") Long checkId, @Param("cutOff") OffsetDateTime cutOff);
}
