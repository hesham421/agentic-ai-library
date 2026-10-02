package io.agenticai.integration.controller;

import io.agenticai.integration.dto.ConfirmedCheckResponse;
import io.agenticai.integration.dto.UploadConfirmationRequest;
import io.agenticai.integration.dto.UploadReceiptResponse;
import io.agenticai.integration.dto.UploadedDocumentResponse;
import io.agenticai.integration.service.UploadConfirmationService;
import io.agenticai.integration.service.UploadService;
import io.agenticai.integration.service.UploadedDocumentsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * Host Integration's upload surface of a {@code manual} Check — hand over an uploaded document
 * (API-INT-002), confirm the uploads (API-INT-003), list the uploaded documents (API-INT-007).
 * Plain JSON, no envelope; every error — including a non-numeric {@code checkId}, a missing
 * multipart part and an upload above the request limit — is answered by
 * {@code IntegrationProblemAdvice} as a ProblemDetail; nothing is mapped here. No authorization
 * (raw-idea A2). Zero logic.
 *
 * <p>The API ids are held in constants and in the Javadoc. The upload declares no {@code consumes}
 * (a handler-mapping 415 would bypass {@code IntegrationProblemAdvice}); its served OpenAPI body is
 * documented as {@code multipart/form-data} by {@code OpenApiGroupsConfiguration}.
 */
@RestController("intCheckUploadController")
@RequestMapping("/api/v1/checks/{checkId}")
public class CheckUploadController {

    /** API-INT-002 — Hand over an uploaded document. */
    public static final String API_INT_002 = "API-INT-002";

    /** API-INT-003 — Confirm the uploads. */
    public static final String API_INT_003 = "API-INT-003";

    /** API-INT-007 — List the uploaded documents of a Check. */
    public static final String API_INT_007 = "API-INT-007";

    private final UploadService uploads;
    private final UploadConfirmationService confirmations;
    private final UploadedDocumentsService uploadedDocuments;

    public CheckUploadController(UploadService uploads,
                                 UploadConfirmationService confirmations,
                                 UploadedDocumentsService uploadedDocuments) {
        this.uploads = Objects.requireNonNull(uploads, "uploads");
        this.confirmations = Objects.requireNonNull(confirmations, "confirmations");
        this.uploadedDocuments = Objects.requireNonNull(uploadedDocuments, "uploadedDocuments");
    }

    /**
     * {@value #API_INT_002} — {@code POST /api/v1/checks/{checkId}/documents}
     * ({@code multipart/form-data}, parts {@code documentType} and {@code file}): hands one file over
     * to Document Access under the Check's own service code and version (201,
     * {@code UploadReceiptResponse}). Errors: INT-400-REQUEST-INVALID, RPT-404-CHECK-NOT-FOUND,
     * INT-409-CHECK-NOT-AWAITING-DOCUMENTS, INT-413-UPLOAD-TOO-LARGE, DOC-400-INCOMPLETE-UPLOAD,
     * DOC-404-SERVICE-VERSION-NOT-FOUND, DOC-422-FETCH-MODE-NOT-MANUAL,
     * DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE, DOC-409-CHECK-ENDED, DOC-422-UPLOAD-LIMIT-REACHED,
     * INT-500. Honours CON-INT-002. Traces: REQ-INT-006 … REQ-INT-017, REQ-INT-019, REQ-INT-053,
     * REQ-INT-065.
     *
     * @param checkId      DBF-INT-001, integer int64
     * @param documentType the document type code (string ≤ 100), as received
     * @param file         exactly one file
     */
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadReceiptResponse upload(@PathVariable("checkId") Long checkId,
                                        @RequestPart("documentType") String documentType,
                                        @RequestPart("file") MultipartFile file) {
        return uploads.upload(checkId, documentType, file);
    }

    /**
     * {@value #API_INT_003} — {@code POST /api/v1/checks/{checkId}/upload-confirmation} (body
     * {@code {}}): confirms that the uploads are complete; the Check continues in the background
     * (202, {@code ConfirmedCheckResponse}). Errors: INT-400-REQUEST-INVALID,
     * CHK-404-CHECK-NOT-FOUND, CHK-409-CHECK-NOT-AWAITING-DOCUMENTS, INT-500. Honours CON-INT-003.
     * Traces: REQ-INT-006 … REQ-INT-008, REQ-INT-018 … REQ-INT-020, REQ-INT-053.
     *
     * @param checkId      DBF-INT-001, integer int64
     * @param confirmation the empty confirmation body
     */
    @PostMapping("/upload-confirmation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ConfirmedCheckResponse confirm(@PathVariable("checkId") Long checkId,
                                          @Valid @RequestBody UploadConfirmationRequest confirmation) {
        return confirmations.confirm(checkId);
    }

    /**
     * {@value #API_INT_007} — {@code GET /api/v1/checks/{checkId}/documents}: the uploaded documents
     * of the Check in upload order, never the content; empty when none (200, array of
     * {@code UploadedDocumentResponse}). Errors: INT-400-REQUEST-INVALID, INT-500. Honours
     * CON-INT-007. Traces: REQ-INT-007, REQ-INT-008, REQ-INT-017, REQ-INT-020, REQ-INT-057,
     * REQ-INT-063.
     *
     * @param checkId DBF-INT-001, integer int64
     */
    @GetMapping("/documents")
    public List<UploadedDocumentResponse> list(@PathVariable("checkId") Long checkId) {
        return uploadedDocuments.list(checkId);
    }
}
