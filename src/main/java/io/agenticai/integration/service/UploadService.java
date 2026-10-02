package io.agenticai.integration.service;

import io.agenticai.doc.contract.UploadReceipt;
import io.agenticai.integration.domain.CheckSnapshot;
import io.agenticai.integration.domain.UploadCommand;
import io.agenticai.integration.domain.UploadGuard;
import io.agenticai.integration.dto.UploadReceiptResponse;
import io.agenticai.integration.error.IntegrationErrorCodes;
import io.agenticai.integration.error.IntegrationException;
import io.agenticai.integration.error.IntegrationTexts;
import io.agenticai.integration.port.CheckRecordPort;
import io.agenticai.integration.port.DocumentAccessPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

/**
 * API-INT-002 — Hand over an uploaded document (CON-INT-002). Steps, in the unit's order:
 * <ol>
 *   <li>size limit — enforced by the container (multipart limits bound to
 *       {@code aias.integration.upload.request-limit}) → {@code INT-413-UPLOAD-TOO-LARGE}, mapped by
 *       the advice; parse → {@code INT-400-REQUEST-INVALID};</li>
 *   <li>load the Check: {@link CheckRecordPort#read} (unknown → {@code RPT-404-CHECK-NOT-FOUND},
 *       passed through — REQ-INT-012);</li>
 *   <li>RULE-INT-001 on its status (DBF-INT-002), delegated to {@link UploadGuard} —
 *       {@code INT-409-CHECK-NOT-AWAITING-DOCUMENTS}; nothing is handed over on refusal;</li>
 *   <li>{@link DocumentAccessPort#handOver} with the Check's OWN service code (DBF-INT-003) and
 *       version (DBF-INT-004) — never the caller's (REQ-INT-010); the file as content, its name as
 *       text (REQ-INT-015). Document Access's refusals pass through unchanged (ADR-INT-025).</li>
 * </ol>
 * The upload never confirms the uploads (REQ-INT-019); each same-type upload is its own handover
 * (REQ-INT-065). The bytes live only for the request (REQ-INT-059) and are never logged.
 *
 * <p>No {@code @Transactional}: INT owns no table; Document Access's handover is its own
 * transaction. Read-then-act is not atomic by design: a Check that ends in between is refused by
 * Document Access ({@code DOC-409-CHECK-ENDED}, ADR-INT-025); INT adds no guard.
 */
@Service("intUploadService")
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    /** The {@code {detail}} key of INT-400-REQUEST-INVALID for an unreadable multipart part (messages.properties). */
    private static final String DETAIL_MULTIPART_UNREADABLE = "INT-DETAIL-MULTIPART-UNREADABLE";

    private final CheckRecordPort checkRecords;
    private final DocumentAccessPort documentAccess;

    public UploadService(CheckRecordPort checkRecords, DocumentAccessPort documentAccess) {
        this.checkRecords = Objects.requireNonNull(checkRecords, "checkRecords");
        this.documentAccess = Objects.requireNonNull(documentAccess, "documentAccess");
    }

    /**
     * Hands one uploaded file over for the Check.
     *
     * @param checkId      DBF-INT-001
     * @param documentType the document type code, as received (checked by Document Access)
     * @param file         the one uploaded file
     * @return Document Access's receipt
     */
    public UploadReceiptResponse upload(Long checkId, String documentType, MultipartFile file) {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(file, "file");

        CheckSnapshot check = checkRecords.read(checkId);
        UploadGuard.forCheck(check).requireUploadAllowed();

        UploadCommand command = new UploadCommand(checkId, documentType, file.getOriginalFilename(), contentOf(file));
        UploadReceipt receipt = documentAccess.handOver(command, check.serviceCode(), check.versionNumber());
        log.info("INT upload handed over checkId={} uploadedDocumentId={} oversized={}",
                checkId, receipt.uploadedDocumentId(), receipt.oversized());
        return UploadReceiptResponse.of(receipt);
    }

    /** The file's content; a part that cannot be read is an unreadable request (REQ-INT-007). */
    private static byte[] contentOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException unreadable) {
            throw new IntegrationException(IntegrationErrorCodes.REQUEST_INVALID, unreadable,
                    IntegrationTexts.english(DETAIL_MULTIPART_UNREADABLE));
        }
    }
}
