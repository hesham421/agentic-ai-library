package io.agenticai.integration.controller;

import io.agenticai.integration.dto.RequiredDocumentTypesResponse;
import io.agenticai.integration.service.RequiredDocumentTypesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Host Integration's read of the required document types of the version a Check runs on
 * (API-INT-008, ADR-INT-020). Plain JSON, no envelope; every error is answered by
 * {@code IntegrationProblemAdvice}. No authorization (raw-idea A2). Zero logic.
 *
 * <p>The API id is held in a constant and in the Javadoc, since springdoc (and so
 * {@code @Operation}) is not on the classpath.
 */
@RestController("intRequiredDocumentTypesController")
@RequestMapping("/api/v1/checks/{checkId}")
public class RequiredDocumentTypesController {

    /** API-INT-008 — Read the required document types of a Check's version. */
    public static final String API_INT_008 = "API-INT-008";

    private final RequiredDocumentTypesService requiredDocumentTypes;

    public RequiredDocumentTypesController(RequiredDocumentTypesService requiredDocumentTypes) {
        this.requiredDocumentTypes = Objects.requireNonNull(requiredDocumentTypes, "requiredDocumentTypes");
    }

    /**
     * {@value #API_INT_008} — {@code GET /api/v1/checks/{checkId}/required-document-types}: the
     * required document types of the service package version the Check runs on, in the version's
     * order (200, {@code RequiredDocumentTypesResponse}). Errors: INT-400-REQUEST-INVALID,
     * RPT-404-CHECK-NOT-FOUND, INT-500. Honours CON-INT-008. Traces: REQ-INT-006 … REQ-INT-008,
     * REQ-INT-016, REQ-INT-020, REQ-INT-057, REQ-INT-064.
     *
     * @param checkId DBF-INT-001, integer int64
     */
    @GetMapping("/required-document-types")
    public RequiredDocumentTypesResponse read(@PathVariable("checkId") Long checkId) {
        return requiredDocumentTypes.read(checkId);
    }
}
