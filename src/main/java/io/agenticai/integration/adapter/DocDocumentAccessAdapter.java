package io.agenticai.integration.adapter;

import io.agenticai.doc.contract.DocumentAccess;
import io.agenticai.doc.contract.UploadReceipt;
import io.agenticai.doc.contract.UploadedDocumentSummary;
import io.agenticai.integration.domain.UploadCommand;
import io.agenticai.integration.port.DocumentAccessPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * {@link DocumentAccessPort} over Document Access's published in-process interface
 * {@link DocumentAccess} (PORTS; REQ-INT-009, REQ-INT-015, REQ-INT-063, REQ-INT-065).
 *
 * <ul>
 *   <li>{@code handOver} calls {@code handOverUpload} with the file's bytes and its name as text —
 *       INT never builds or opens a file path (REQ-INT-015). Every upload is its own hand-over;
 *       nothing is removed, replaced or chosen among (REQ-INT-065).</li>
 *   <li>{@code listUploaded} calls {@code listUploadedDocuments} (ADR-INT-023) and returns its list
 *       unchanged — never the content, never against DOC's table.</li>
 * </ul>
 * Document Access's six upload refusals are not caught: they reach {@code IntegrationProblemAdvice}
 * unchanged (ADR-INT-003, ADR-INT-025). Stateless — the bytes are not kept (REQ-INT-059).
 */
@Component("intDocDocumentAccessAdapter")
public class DocDocumentAccessAdapter implements DocumentAccessPort {

    private final DocumentAccess documentAccess;

    public DocDocumentAccessAdapter(DocumentAccess documentAccess) {
        this.documentAccess = Objects.requireNonNull(documentAccess, "documentAccess");
    }

    @Override
    public UploadReceipt handOver(UploadCommand command, String serviceCode, int versionNumber) {
        Objects.requireNonNull(command, "command");
        return documentAccess.handOverUpload(command.checkId(), serviceCode, versionNumber,
                command.documentType(), command.fileName(), command.bytes());
    }

    @Override
    public List<UploadedDocumentSummary> listUploaded(Long checkId) {
        return documentAccess.listUploadedDocuments(Objects.requireNonNull(checkId, "checkId"));
    }
}
