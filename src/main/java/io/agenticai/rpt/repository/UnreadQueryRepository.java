package io.agenticai.rpt.repository;

import io.agenticai.rpt.entity.UnreadQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence of ENT-RPT-004 Unread Query ({@code RPT_UNREAD_QUERY}). Module-internal.
 *
 * <p>Exactly the operations SVC-API names: QR-RPT-004 (READ_ONLY) and the {@code completeCheck} INSERT of
 * the unread queries (inherited {@code saveAllAndFlush}). Rows leave only with their Check Run, by the
 * database's {@code ON DELETE CASCADE} (the purge); they are never updated (REQ-RPT-020).
 */
public interface UnreadQueryRepository extends JpaRepository<UnreadQuery, Long> {

    /**
     * QR-RPT-004 — {@code SELECT POSITION, QUERY_NAME, DETAIL FROM RPT_UNREAD_QUERY WHERE CHECK_RUN_ID = :checkId ORDER BY POSITION} (UQ_RPT_UNREAD_QUERY_RUN_POS), READ_ONLY. The entity is read; its audit columns are never
     * exposed.
     */
    @Transactional(readOnly = true)
    @Query("SELECT x FROM UnreadQuery x WHERE x.checkRunId = :checkId ORDER BY x.position")
    List<UnreadQuery> findOfCheckRun(@Param("checkId") Long checkId);
}
