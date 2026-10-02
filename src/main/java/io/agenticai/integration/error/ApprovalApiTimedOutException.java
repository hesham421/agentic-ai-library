package io.agenticai.integration.error;

/**
 * {@code INT-504-APPROVAL-API-TIMED-OUT} — the host Approval API did not answer within
 * {@code aias.integration.approval.timeout} (REQ-INT-037). Nothing is recorded; the employee may
 * submit again (REQ-INT-038).
 */
public class ApprovalApiTimedOutException extends IntegrationException {

    /**
     * @param seconds the approval timeout in whole seconds (the catalog's {@code {timeout}})
     * @param cause   the original timeout, kept as the cause
     */
    public ApprovalApiTimedOutException(long seconds, Throwable cause) {
        super(IntegrationErrorCodes.APPROVAL_API_TIMED_OUT, cause, seconds);
    }
}
