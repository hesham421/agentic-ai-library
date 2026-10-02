package io.agenticai.doc.repository;

import io.agenticai.doc.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Persistence of ENT-DOC-001 Uploaded Document ({@code DOC_UPLOADED_DOC}) — the plan's
 * {@code UploadedDocumentStore}, named {@code …Repository} after the layer contract and REG's
 * precedent. Module-internal: injected only by the DOC services; other modules reach an upload
 * only through the in-process {@code DocumentAccess} interface (CON-DOC-003 … CON-DOC-006).
 *
 * <p>Exactly the operations the plan names: save (the inherited {@code save} of a new instance —
 * the handover's insert, SVC-API handOverUpload step 6), read by CHECK_ID, count by CHECK_ID,
 * delete by CHECK_ID, delete the rows of ended Checks. There is no update of any kind
 * (RULE-DOC-004, REQ-DOC-023): the entity is immutable and this interface declares nothing that
 * changes a row. Every read filters on {@code CHECK_ID} (RULE-DOC-008) and runs under the
 * calling service's read-only transaction; the deletes run under its write transaction.
 */
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, Long> {

    /**
     * QR-DOC-001 — the Uploaded Documents of a Check, without content (API-DOC-001 through
     * {@code listUploadedDocuments}, CON-DOC-006; REQ-DOC-064):
     * {@code SELECT UPLOADED_DOCUMENT_ID, DOCUMENT_TYPE, FILE_NAME, FILE_SIZE, OVERSIZED, CREATED_AT
     * FROM DOC_UPLOADED_DOC WHERE CHECK_ID = :checkId ORDER BY CREATED_AT}. A projection — the
     * {@code CONTENT} column is never selected. Empty for a Check without uploads.
     */
    @Query("""
            select new io.agenticai.doc.repository.UploadedDocumentListing(
                u.uploadedDocumentId, u.documentType, u.fileName, u.fileSize, u.oversized, u.createdAt)
            from UploadedDocument u
            where u.checkId = :checkId
            order by u.createdAt
            """)
    List<UploadedDocumentListing> findListingByCheckIdOrderByCreatedAtAsc(@Param("checkId") Long checkId);

    /**
     * The fetch read of {@code manual} mode (SVC-API fetchDocuments step 4; REQ-DOC-018,
     * REQ-DOC-056, RULE-DOC-008): every Uploaded Document of the Check, content included,
     * {@code WHERE CHECK_ID = :checkId ORDER BY CREATED_AT}. The content is read only for the
     * fetching call and kept by nothing after it returns (REQ-DOC-055, ADR-DOC-008).
     */
    List<UploadedDocument> findByCheckIdOrderByCreatedAtAsc(Long checkId);

    /**
     * RULE-DOC-010 (SVC-API handOverUpload step 5a; REQ-DOC-063, ADR-DOC-016):
     * {@code SELECT COUNT(*) FROM DOC_UPLOADED_DOC WHERE CHECK_ID = :checkId} — a count query, the
     * rows are never loaded; oversized rows count.
     */
    long countByCheckId(Long checkId);

    /**
     * The end of a Check (SVC-API endCheck step 2; REQ-DOC-054, ADR-DOC-008):
     * {@code DELETE FROM DOC_UPLOADED_DOC WHERE CHECK_ID = :checkId} — a hard delete in one bulk
     * statement. Flushes pending writes first and clears the persistence context after, so the
     * statement sees the transaction's own writes and no deleted row lingers as a managed
     * instance.
     *
     * @return the rows deleted — part of {@code deletedCount} (CON-DOC-005)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UploadedDocument u where u.checkId = :checkId")
    int deleteByCheckId(@Param("checkId") Long checkId);

    /**
     * The sweep of late uploads (SVC-API endCheck step 3; REQ-DOC-062, ADR-DOC-015):
     * {@code DELETE FROM DOC_UPLOADED_DOC u WHERE EXISTS (SELECT 1 FROM DOC_ENDED_CHECK e WHERE
     * e.CHECK_ID = u.CHECK_ID)} — removes every Uploaded Document whose Check is recorded as an
     * Ended Check, including the one left by a handover that committed after its Check's own
     * delete. Flushes first so the Ended Check recorded in the same transaction (step 1) is seen.
     *
     * @return the rows deleted — part of {@code deletedCount} (CON-DOC-005)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UploadedDocument u where exists (select 1 from EndedCheck e where e.checkId = u.checkId)")
    int deleteOfEndedChecks();
}
