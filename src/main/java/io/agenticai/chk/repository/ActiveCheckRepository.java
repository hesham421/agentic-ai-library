package io.agenticai.chk.repository;

import io.agenticai.chk.entity.ActiveCheck;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of ENT-CHK-001 Active Check ({@code CHK_ACTIVE_CHECK}). Module-internal.
 *
 * <p>Exactly the operations SVC-API names: QR-CHK-001 (the one catalogued query, API-CHK-001) and
 * the in-process operations written inline in the procedures (ADR-CHK-017 (3)) — the start's
 * INSERT (inherited {@code saveAndFlush}), the confirmation's locking read and UPDATE, the deadline
 * check's read, the ending's DELETE and the start-up recovery's DELETE. Nothing else is called.
 */
public interface ActiveCheckRepository extends JpaRepository<ActiveCheck, Long> {

    /**
     * QR-CHK-001 — {@code SELECT CHECK_ID, CHECK_STATUS, DEADLINE_AT FROM CHK_ACTIVE_CHECK WHERE
     * CHECK_ID = :checkId}, READ_ONLY, served by {@code UQ_CHK_ACTIVE_CHECK_CHECK_ID}.
     */
    @Transactional(readOnly = true)
    @Query("SELECT new io.agenticai.chk.repository.ActiveCheckRow(a.checkId, a.checkStatus, a.deadlineAt) "
            + "FROM ActiveCheck a WHERE a.checkId = :checkId")
    Optional<ActiveCheckRow> findRowByCheckId(@Param("checkId") Long checkId);

    /**
     * confirmUploads step 1 — {@code SELECT … FROM CHK_ACTIVE_CHECK WHERE CHECK_ID = :checkId FOR
     * UPDATE}: the locking read that serialises the confirmation against the deadline check
     * (RULE-CHK-010). Runs inside the caller's READ_WRITE transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ActiveCheck a WHERE a.checkId = :checkId")
    Optional<ActiveCheck> findForUpdateByCheckId(@Param("checkId") Long checkId);

    /**
     * confirmUploads step 3 — {@code UPDATE CHK_ACTIVE_CHECK SET CHECK_STATUS = :checkStatus,
     * DEADLINE_AT = :deadlineAt, UPDATED_AT = SYSTIMESTAMP WHERE CHECK_ID = :checkId}
     * (REQ-CHK-077). Native, because {@code UPDATED_AT} is mapped read-only on the entity and the
     * plan prescribes the database's own {@code SYSTIMESTAMP} (oracle19c); all values are bound.
     *
     * @return the rows updated
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE CHK_ACTIVE_CHECK SET CHECK_STATUS = :checkStatus, DEADLINE_AT = :deadlineAt, "
            + "UPDATED_AT = SYSTIMESTAMP WHERE CHECK_ID = :checkId", nativeQuery = true)
    int confirmRunning(@Param("checkId") Long checkId,
                       @Param("checkStatus") String checkStatus,
                       @Param("deadlineAt") OffsetDateTime deadlineAt);

    /**
     * The deadline check's read — {@code SELECT CHECK_ID, CHECK_STATUS FROM CHK_ACTIVE_CHECK WHERE
     * DEADLINE_AT <= :now} (REQ-CHK-080), READ_ONLY, served by {@code IDX_CHK_ACTIVE_CHECK_DEADLINE_AT}.
     */
    @Transactional(readOnly = true)
    @Query("SELECT new io.agenticai.chk.repository.ActiveCheckRow(a.checkId, a.checkStatus, a.deadlineAt) "
            + "FROM ActiveCheck a WHERE a.deadlineAt <= :now")
    List<ActiveCheckRow> findDue(@Param("now") OffsetDateTime now);

    /**
     * Every ending's guard — {@code DELETE FROM CHK_ACTIVE_CHECK WHERE CHECK_ID = :checkId}
     * (REQ-CHK-078, REQ-CHK-079): exactly one of two racing ending paths deletes the row.
     *
     * @return the rows deleted — 0 when another path already ended the Check
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ActiveCheck a WHERE a.checkId = :checkId")
    int deleteByCheckId(@Param("checkId") Long checkId);

    /**
     * Start-up recovery step 2 — {@code DELETE FROM CHK_ACTIVE_CHECK}: every row left by an
     * earlier run (REQ-CHK-081).
     *
     * @return the rows deleted
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ActiveCheck a")
    int deleteEveryActiveCheck();
}
