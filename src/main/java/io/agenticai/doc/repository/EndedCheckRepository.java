package io.agenticai.doc.repository;

import io.agenticai.doc.entity.EndedCheck;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of ENT-DOC-002 Ended Check ({@code DOC_ENDED_CHECK}) — the plan's
 * {@code EndedCheckStore}, named {@code …Repository} after the layer contract and REG's
 * precedent. Module-internal.
 *
 * <p>Exactly the operations the plan names (ADR-DOC-015): exists by CHECK_ID, and insert if
 * absent — the end-of-Check procedure's exists-then-{@code save} of a new instance under
 * {@code UQ_DOC_ENDED_CHECK_CHECK_ID}, which turns a concurrent duplicate into "already
 * recorded" (SVC-API endCheck step 1). No update and no delete: the entity is immutable, and
 * this interface declares nothing that changes or removes a row.
 */
public interface EndedCheckRepository extends JpaRepository<EndedCheck, Long> {

    /**
     * Whether the Check is recorded as ended — RULE-DOC-009 (SVC-API handOverUpload step 1a,
     * REQ-DOC-061) and the insert-if-absent of endCheck step 1 (REQ-DOC-060):
     * {@code SELECT 1 FROM DOC_ENDED_CHECK WHERE CHECK_ID = :checkId}, served by
     * {@code UQ_DOC_ENDED_CHECK_CHECK_ID}. An existence check, the row is never loaded.
     */
    boolean existsByCheckId(Long checkId);
}
