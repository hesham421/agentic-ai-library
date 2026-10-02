package io.agenticai.integration.dto;

import java.util.List;
import java.util.Objects;

/**
 * The API document's {@code RequiredDocumentTypesResponse} schema (API-INT-008), field for field.
 *
 * @param checkId               DBF-INT-001 — the Check identifier, integer int64
 * @param serviceCode           DBF-INT-003 — the Check's service code
 * @param versionNumber         DBF-INT-004 — the version the Check runs on
 * @param requiredDocumentTypes that version's required document types (each string ≤ 100), exactly
 *                              as stored, in the version's order; unmodifiable
 */
public record RequiredDocumentTypesResponse(Long checkId,
                                            String serviceCode,
                                            Integer versionNumber,
                                            List<String> requiredDocumentTypes) {

    public RequiredDocumentTypesResponse {
        requiredDocumentTypes = List.copyOf(Objects.requireNonNull(requiredDocumentTypes, "requiredDocumentTypes"));
    }
}
