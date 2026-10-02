package io.agenticai.integration.port;

import io.agenticai.integration.domain.ApprovalDefinition;

/**
 * The host Approval API definition of the service package version a Check ran on (REQ-INT-025,
 * REQ-INT-028, REQ-INT-030). Implemented over the Service Registry's approval interface; injected
 * only into the Employee Decision service (REQ-INT-029, AIAS-4) and never exposed to a model.
 */
public interface ApprovalDefinitionPort {

    /**
     * @param serviceCode   the Check's service code (DBF-INT-003)
     * @param versionNumber the Check's version number (DBF-INT-004)
     * @return whether the version enables the Approval API and, when it does, its method and path
     */
    ApprovalDefinition definitionOf(String serviceCode, int versionNumber);
}
