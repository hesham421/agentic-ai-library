package io.agenticai.rpt.repository;

import io.agenticai.rpt.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence of ENT-RPT-002 Finding ({@code RPT_FINDING}). Module-internal.
 *
 * <p>Exactly the operations SVC-API names: QR-RPT-002 (READ_ONLY) and the {@code completeCheck} INSERT of
 * the findings (inherited {@code saveAllAndFlush}). Rows leave only with their Check Run, by the
 * database's {@code ON DELETE CASCADE} (the purge); they are never updated (REQ-RPT-020).
 */
public interface FindingRepository extends JpaRepository<Finding, Long> {

    /**
     * QR-RPT-002 — {@code SELECT POSITION, CONDITION_TEXT, FINDING_OUTCOME, EVIDENCE, NOTE FROM RPT_FINDING WHERE CHECK_RUN_ID = :checkId ORDER BY POSITION} (UQ_RPT_FINDING_RUN_POS), READ_ONLY. The entity is read; its audit columns are never
     * exposed.
     */
    @Transactional(readOnly = true)
    @Query("SELECT x FROM Finding x WHERE x.checkRunId = :checkId ORDER BY x.position")
    List<Finding> findOfCheckRun(@Param("checkId") Long checkId);
}
