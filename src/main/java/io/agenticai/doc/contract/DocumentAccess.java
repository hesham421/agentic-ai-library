package io.agenticai.doc.contract;

import java.time.Instant;
import java.util.List;

/**
 * The in-process interface of Document Access (DOC) that INT and CHK are given (contract-doc.md;
 * profile {@code module_interface: in_process}). This package is the only DOC package another
 * module may import. Every value object is an immutable record with unmodifiable lists; no
 * entity crosses this boundary (the Uploaded Document and the Ended Check are PRIVATE).
 *
 * <p>The four operations are CON-DOC-003 … CON-DOC-006. Refusals are the typed exceptions of
 * this package, each carrying an in-process code of {@link DocumentRejectionCodes} and the SRS
 * rule message (ADR-DOC-012); INT maps them to its own ProblemDetail. No caller authentication
 * in this version (raw-idea A2).
 */
public interface DocumentAccess {

    /**
     * CON-DOC-003 — hands over a file uploaded for a Check (INT → DOC): a new Uploaded Document
     * holding the file, validated against the Check's service package version (REQ-DOC-017,
     * ADR-DOC-006). An oversized file is kept without content and later reported UNREADABLE /
     * TOO_LARGE (RULE-DOC-005). Every handover is a new row; an earlier upload is never changed
     * (REQ-DOC-023).
     *
     * @param checkId       the Check's identifier (a value — DOC never reads the Check)
     * @param serviceCode   the service code of the Check
     * @param versionNumber the service package version of the Check
     * @param documentType  the document type the employee gave
     * @param fileName      the file name as uploaded
     * @param bytes         the file content
     * @return the receipt, with the RULE-DOC-005 notice when oversized
     * @throws IncompleteUploadException         RULE-DOC-003 — no Check, no document type or an empty file
     * @throws CheckEndedException               RULE-DOC-009 — the Check is recorded as ended
     * @throws ServiceVersionNotFoundException   REQ-DOC-003 — no such service package version
     * @throws FetchModeNotManualException       RULE-DOC-001 — the version's fetch mode is not {@code manual}
     * @throws DocumentTypeNotOfServiceException RULE-DOC-002 — the type is not one of the version's required types
     * @throws UploadLimitReachedException       RULE-DOC-010 — the Check already holds the maximum uploads
     */
    UploadReceipt handOverUpload(Long checkId,
                                 String serviceCode,
                                 int versionNumber,
                                 String documentType,
                                 String fileName,
                                 byte[] bytes);

    /**
     * CON-DOC-004 — fetches and reads the documents of a Check (CHK → DOC) by the fetch mode of
     * its service package version, and nothing else (REQ-DOC-001): exactly one outcome per
     * fetched or uploaded document plus one MISSING outcome per required document type no
     * document carries (REQ-DOC-034, REQ-DOC-035); a failure on one document never stops the
     * others (REQ-DOC-037); a failed or over-limit document source query yields UNREADABLE /
     * SOURCE_QUERY_FAILED for every required type, never an error (REQ-DOC-039). DOC keeps no
     * document and no content after it returns (REQ-DOC-055).
     *
     * @param checkId       the Check's identifier
     * @param requestNumber the Check's request number, bound to the document source query
     * @param serviceCode   the service code of the Check
     * @param versionNumber the service package version the Check pinned
     * @param deadline      the instant by which the Check must end (REQ-DOC-040): a document not
     *                      read by then is UNREADABLE / OUT_OF_TIME
     * @return the outcomes, unmodifiable; content only on READ outcomes
     * @throws ServiceVersionNotFoundException REQ-DOC-003 — no such service package version; no
     *                                         document is fetched
     */
    List<DocumentOutcome> fetchDocuments(Long checkId,
                                         String requestNumber,
                                         String serviceCode,
                                         int versionNumber,
                                         Instant deadline);

    /**
     * CON-DOC-005 — ends a Check (CHK → DOC): records the Check as ended, hard-deletes its
     * Uploaded Documents, and sweeps the Uploaded Documents of every Check already recorded as
     * ended (REQ-DOC-054, REQ-DOC-060, REQ-DOC-062, ADR-DOC-015). Idempotent: repeating the call
     * records nothing new and deletes 0 rows. Raises no refusal.
     *
     * @param checkId the Check's identifier
     * @return the rows deleted by the call
     */
    EndCheckResult endCheck(Long checkId);

    /**
     * CON-DOC-006 — the Uploaded Documents carrying the Check's identifier, ordered by upload
     * time, never their content (REQ-DOC-064, RULE-DOC-008). A Check with no upload — an
     * unknown or ended one included — answers an empty list.
     *
     * @param checkId the Check's identifier
     * @return the summaries, unmodifiable
     * @throws CheckIdRequiredException {@code DOC-400-CHECK-ID-REQUIRED} — {@code checkId} is absent
     */
    List<UploadedDocumentSummary> listUploadedDocuments(Long checkId);
}
