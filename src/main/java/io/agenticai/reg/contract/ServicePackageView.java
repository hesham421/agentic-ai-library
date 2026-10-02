package io.agenticai.reg.contract;

import java.util.List;
import java.util.Objects;

/**
 * CON-REG-007 — the current service package of a service as supplied to a Check: the service
 * knowledge as its own part, apart from the queries and the document settings (REQ-REG-018,
 * REQ-REG-030). Immutable (REQ-REG-060): a record whose lists are unmodifiable. Carries no
 * approval API definition (REQ-REG-044), no service definition text and no request data
 * (REQ-REG-059).
 *
 * @param serviceCode           the canonical service code
 * @param versionNumber         the version supplied — the number a Check pins (ADR-REG-020)
 * @param serviceKnowledge      the whole service knowledge text, unaltered (REQ-REG-027)
 * @param inputName             the name of the only bind parameter of every query (REQ-REG-032)
 * @param queries               the queries of the version, by query name; unmodifiable
 * @param fetchMode             {@code path}, {@code blob} or {@code manual} — the profile's closed
 *                              enum, carried as its stored value (CON-REG-006)
 * @param documentSource        the document source of a {@code path} / {@code blob} version;
 *                              {@code null} for {@code manual}
 * @param requiredDocumentTypes the required document types, by document type; unmodifiable
 */
public record ServicePackageView(String serviceCode,
                                 int versionNumber,
                                 String serviceKnowledge,
                                 String inputName,
                                 List<QueryDefinition> queries,
                                 String fetchMode,
                                 DocumentSource documentSource,
                                 List<String> requiredDocumentTypes) {

    public ServicePackageView {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(serviceKnowledge, "serviceKnowledge");
        Objects.requireNonNull(inputName, "inputName");
        Objects.requireNonNull(fetchMode, "fetchMode");
        queries = queries == null ? List.of() : List.copyOf(queries);
        requiredDocumentTypes = requiredDocumentTypes == null ? List.of() : List.copyOf(requiredDocumentTypes);
    }
}
