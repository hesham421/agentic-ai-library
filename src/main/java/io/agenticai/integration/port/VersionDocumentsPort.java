package io.agenticai.integration.port;

import java.util.List;

/**
 * The required document types of the service package version a Check runs on (REQ-INT-064,
 * ADR-INT-020). Injected only into the required-document-types read service.
 */
public interface VersionDocumentsPort {

    /**
     * @param serviceCode   the Check's service code (DBF-INT-003)
     * @param versionNumber the Check's version number (DBF-INT-004)
     * @return the version's required document types exactly as stored, in declared order;
     *         unmodifiable
     */
    List<String> requiredDocumentTypes(String serviceCode, int versionNumber);
}
