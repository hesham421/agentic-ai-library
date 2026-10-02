package io.agenticai.chk.port;

import java.time.Instant;
import java.util.List;

/**
 * The Check Engine's only way to documents (REQ-CHK-017, REQ-CHK-018): the documents of a Check,
 * their read status and their content come from Document Access, and the end of every Check is
 * reported back to it (REQ-CHK-061, REQ-CHK-062). CHK opens no host file, host BLOB column or
 * uploaded file itself; the storage root and the maximum file size are applied inside Document
 * Access. No implementation of this port is ever handed to the comparison model as a tool.
 */
public interface DocumentPort {

    /**
     * Fetches and reads the documents of a Check (REQ-CHK-017): one outcome per fetched or
     * uploaded document plus one MISSING outcome per required document type no document carries.
     * An unreadable document is an outcome, never a failure.
     *
     * @param checkId       the Check's identifier
     * @param requestNumber the Check's request number, exactly as received
     * @param serviceCode   the Check's service code
     * @param versionNumber the service package version the Check pinned
     * @param deadline      the instant by which the Check must end; a document not read by then
     *                      is reported UNREADABLE / OUT_OF_TIME by Document Access
     * @return the outcomes, unmodifiable
     * @throws DocumentFetchFailedException the fetch answered a failure instead of outcomes
     *                                      (e.g. service package version not found) — the Check
     *                                      ends FAILED / INTERNAL_ERROR with the failure text as
     *                                      detail (REQ-CHK-023)
     */
    List<DocumentOutcome> fetch(Long checkId,
                                String requestNumber,
                                String serviceCode,
                                int versionNumber,
                                Instant deadline);

    /**
     * Tells Document Access the Check has ended, on every ending path — COMPLETED or FAILED
     * (REQ-CHK-061). Never fails the caller: a failed notice is sent once more and a second
     * failure is recorded in the service log; the Check keeps its ending (REQ-CHK-062).
     *
     * @param checkId the Check's identifier
     */
    void endCheck(Long checkId);
}
