package io.agenticai.doc.controller;

import io.agenticai.doc.dto.UploadedDocumentSummaryResponse;
import io.agenticai.doc.service.UploadedDocumentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * The HTTP surface of Document Access (DOC) — the one read operation of {@code api-spec-doc.yaml},
 * and nothing else (ADR-DOC-011: no POST, PUT, PATCH or DELETE). Plain JSON, no envelope; every
 * error is answered by {@code DocumentAccessProblemAdvice} as a ProblemDetail — nothing is mapped
 * here. No authorization: caller authentication is deferred (raw-idea A2). Zero logic: the call
 * is delegated as is and the result mapped to the documented schema.
 *
 * <p>The API id is held in a constant and in the Javadoc, since springdoc (and so
 * {@code @Operation}) is not on the classpath.
 */
@RestController
@RequestMapping("/api/v1")
public class UploadedDocumentController {

    /** API-DOC-001 — List the Uploaded Documents of a Check. */
    public static final String API_DOC_001 = "API-DOC-001";

    private final UploadedDocumentQueryService queryService;

    public UploadedDocumentController(UploadedDocumentQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    /**
     * {@value #API_DOC_001} — {@code GET /api/v1/uploaded-documents?checkId=}: the Uploaded
     * Documents of the Check as {@code UploadedDocumentSummary[]}, ordered by upload time, empty
     * when none (200). Errors: {@code DOC-400-CHECK-ID-REQUIRED} — {@code checkId} missing or not
     * a number, raised by Spring MVC's parameter binding and mapped by the advice;
     * {@code DOC-500}. Traces: REQ-DOC-017, REQ-DOC-018, REQ-DOC-043, REQ-DOC-056, REQ-DOC-064;
     * DBF-DOC-001, 002, 005, 006, 007, 009, 010. Honours CON-DOC-006 through
     * {@code listUploadedDocuments}.
     *
     * @param checkId the Check identifier (DBF-DOC-002), required
     */
    @GetMapping("/uploaded-documents")
    public List<UploadedDocumentSummaryResponse> listUploadedDocuments(@RequestParam("checkId") Long checkId) {
        return queryService.listUploadedDocuments(checkId).stream()
                .map(UploadedDocumentSummaryResponse::of)
                .toList();
    }
}
