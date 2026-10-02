package io.agenticai.rpt.repository;

import io.agenticai.rpt.entity.CheckDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence of ENT-RPT-003 Check Document ({@code RPT_CHECK_DOCUMENT}). Module-internal.
 *
 * <p>Exactly the operations SVC-API names: QR-RPT-003 (READ_ONLY) and the {@code completeCheck} INSERT of
 * the document outcomes (inherited {@code saveAllAndFlush}). Rows leave only with their Check Run, by the
 * database's {@code ON DELETE CASCADE} (the purge); they are never updated (REQ-RPT-020).
 */
public interface CheckDocumentRepository extends JpaRepository<CheckDocument, Long> {

    /**
     * QR-RPT-003 — {@code SELECT POSITION, DOCUMENT_TYPE, SOURCE_MODE, READ_STATUS, UNREADABLE_REASON, DETAIL FROM RPT_CHECK_DOCUMENT WHERE CHECK_RUN_ID = :checkId ORDER BY POSITION} (UQ_RPT_CHECK_DOCUMENT_RUN_POS), READ_ONLY. The entity is read; its audit columns are never
     * exposed.
     */
    @Transactional(readOnly = true)
    @Query("SELECT x FROM CheckDocument x WHERE x.checkRunId = :checkId ORDER BY x.position")
    List<CheckDocument> findOfCheckRun(@Param("checkId") Long checkId);
}
