package io.agenticai.reg.repository;

import io.agenticai.reg.entity.LoadResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Persistence of ENT-REG-006 Load Result ({@code REG_LOAD_RESULT}) — the load report.
 * Module-internal. The load run inserts rows through {@code save} and clears the previous run
 * through the inherited {@code deleteAllInBatch} (REQ-REG-009, step 1); the exclusive table lock
 * of step 0 is taken by the load run itself, not through this repository.
 */
public interface LoadResultRepository extends JpaRepository<LoadResult, Long> {

    /**
     * QR-REG-005 — every row of the most recent load run:
     * {@code WHERE LOAD_RUN_AT = (SELECT MAX(LOAD_RUN_AT) FROM REG_LOAD_RESULT) ORDER BY SUBJECT_KIND, SUBJECT_NAME}
     * (API-REG-003). Always one complete run: readers see the last committed run only
     * (ADR-REG-015).
     */
    @Query("""
            select r from LoadResult r
            where r.loadRunAt = (select max(x.loadRunAt) from LoadResult x)
            order by r.subjectKind, r.subjectName
            """)
    List<LoadResult> findLatestRun();
}
