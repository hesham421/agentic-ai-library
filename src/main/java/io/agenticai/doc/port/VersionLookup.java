package io.agenticai.doc.port;

import io.agenticai.doc.contract.ServiceVersionNotFoundException;

/**
 * Outbound port of DOC to the Service Registry for one stored service package version: its fetch
 * mode, its document source and its required document types (REQ-DOC-002; CON-REG-009,
 * CON-REG-003, CON-REG-004). The handover reads it for RULE-DOC-001 and RULE-DOC-002, the fetch
 * for its first step. The adapter behind it is the only DOC class that calls the registry's
 * in-process interface; it maps the registry's view into DOC's own
 * {@link VersionDocumentSettings} and translates the registry's not-found refusal.
 */
public interface VersionLookup {

    /**
     * The document settings of the exact stored version.
     *
     * @param serviceCode   the service code of the Check
     * @param versionNumber the service package version of the Check
     * @throws ServiceVersionNotFoundException when the registry holds no such service code or
     *                                         version (REQ-DOC-003)
     */
    VersionDocumentSettings find(String serviceCode, int versionNumber);
}
