package io.agenticai.reg.contract;

import java.util.List;
import java.util.Objects;

/**
 * CON-REG-009 — one stored version, resolved by service code and version number: everything
 * {@link ServicePackageView} carries plus the service definition text and the approval flag
 * (REQ-REG-025, REQ-REG-070, ADR-REG-020). Immutable (REQ-REG-060). The approval API definition
 * itself is not here (REQ-REG-044 — see {@link ApprovalApiRegistry}).
 *
 * @param serviceCode           the canonical service code
 * @param versionNumber         the version number
 * @param serviceKnowledge      the whole service knowledge text, unaltered
 * @param serviceDefinition     the service definition text as loaded
 * @param inputName             the name of the only bind parameter of every query
 * @param queries               the queries of the version, by query name; unmodifiable
 * @param fetchMode             {@code path}, {@code blob} or {@code manual} (stored value)
 * @param documentSource        the document source of a {@code path} / {@code blob} version;
 *                              {@code null} for {@code manual}
 * @param requiredDocumentTypes the required document types, by document type; unmodifiable
 * @param approvalEnabled       whether the version enables the approval API
 */
public record ServicePackageVersionView(String serviceCode,
                                        int versionNumber,
                                        String serviceKnowledge,
                                        String serviceDefinition,
                                        String inputName,
                                        List<QueryDefinition> queries,
                                        String fetchMode,
                                        DocumentSource documentSource,
                                        List<String> requiredDocumentTypes,
                                        boolean approvalEnabled) {

    public ServicePackageVersionView {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(serviceKnowledge, "serviceKnowledge");
        Objects.requireNonNull(serviceDefinition, "serviceDefinition");
        Objects.requireNonNull(inputName, "inputName");
        Objects.requireNonNull(fetchMode, "fetchMode");
        queries = queries == null ? List.of() : List.copyOf(queries);
        requiredDocumentTypes = requiredDocumentTypes == null ? List.of() : List.copyOf(requiredDocumentTypes);
    }
}
