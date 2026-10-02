package io.agenticai.chk.contract;

/**
 * The in-process interface of the Check Engine (CHK) that INT is given (contract-chk.md; profile
 * {@code module_interface: in_process}). This package is the only CHK package another module may
 * import. Every value object is an immutable record; the closed lookups travel as their codes
 * (CON-CHK-001 … CON-CHK-003). No entity crosses this boundary.
 *
 * <p>Refusals are the typed exceptions of this package, each carrying an in-process code of
 * {@link CheckRejectionCodes} and the SRS message (ADR-CHK-018); INT maps them to its own
 * ProblemDetail. No caller authentication in this version (raw-idea A2).
 */
public interface CheckEngine {

    /**
     * CON-CHK-004 — starts a Check and returns at once; the pipeline of a {@code path} /
     * {@code blob} Check runs in the background (REQ-CHK-001, REQ-CHK-003), a {@code manual} Check
     * waits for its uploads (REQ-CHK-056). The request number and the employee identity are kept
     * exactly as sent (REQ-CHK-002); a second Check of the same request is a new, independent
     * Check (REQ-CHK-066).
     *
     * @throws StartIncompleteException          REQ-CHK-004 — a value absent or blank
     * @throws ServiceNotAvailableException      RULE-CHK-001 — the service is not available
     * @throws ConnectionNotActivatedException   REQ-CHK-006 — a connection is not activated
     */
    StartedCheck startCheck(String serviceCode, String requestNumber, String employeeId);

    /**
     * CON-CHK-005 — confirms that the uploads of an AWAITING_DOCUMENTS Check are complete: the
     * Check becomes RUNNING and its pipeline runs on the Check's recorded version (REQ-CHK-057,
     * RULE-CHK-007).
     *
     * @throws CheckNotFoundException             REQ-CHK-058 — the result port knows no such Check
     * @throws CheckNotAwaitingDocumentsException RULE-CHK-010 — the Check is not AWAITING_DOCUMENTS
     */
    ConfirmedCheck confirmUploads(Long checkId);
}
