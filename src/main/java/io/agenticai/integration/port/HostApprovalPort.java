package io.agenticai.integration.port;

import io.agenticai.integration.domain.ApprovalDefinition;

/**
 * The host Approval API — INT's only network call (ADR-INT-009, ADR-INT-017 (5)). Called exactly
 * once per decision request, never retried (REQ-INT-033). Injected only into the Employee Decision
 * service (REQ-INT-029; AIAS-3, AIAS-4); never a model tool.
 */
public interface HostApprovalPort {

    /**
     * Calls the Approval API of {@code definition} for the request.
     *
     * @param definition    an enabled definition (method and path template)
     * @param requestNumber the Check's request number, placed into the path as one encoded value
     * @param checkId       the Check identifier, sent in the body
     * @param decidedBy     the deciding employee, sent in the body
     * @throws io.agenticai.integration.error.ApprovalApiFailedException   any status outside 2xx,
     *         a connection failure or no configured base address ({@code INT-502-APPROVAL-API-FAILED})
     * @throws io.agenticai.integration.error.ApprovalApiTimedOutException no answer within the
     *         approval timeout ({@code INT-504-APPROVAL-API-TIMED-OUT})
     */
    void approve(ApprovalDefinition definition, String requestNumber, Long checkId, String decidedBy);
}
