package io.agenticai.integration.error;

/**
 * {@code INT-502-APPROVAL-API-FAILED} — the host Approval API answered outside 2xx, could not be
 * reached, or has no configured base address (REQ-INT-036). Nothing is recorded; the employee may
 * submit again (REQ-INT-038).
 */
public class ApprovalApiFailedException extends IntegrationException {

    /**
     * @param status the catalog's {@code {status}} value: the HTTP status the host answered, or the
     *               text saying why there was no answer
     * @param cause  the original failure, or {@code null}
     */
    public ApprovalApiFailedException(String status, Throwable cause) {
        super(IntegrationErrorCodes.APPROVAL_API_FAILED, cause, status);
    }
}
