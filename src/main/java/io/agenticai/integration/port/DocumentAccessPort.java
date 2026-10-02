package io.agenticai.integration.port;

import io.agenticai.doc.contract.UploadReceipt;
import io.agenticai.doc.contract.UploadedDocumentSummary;
import io.agenticai.integration.domain.UploadCommand;

import java.util.List;

/**
 * INT's port to Document Access (PORTS; REQ-INT-009, REQ-INT-015, REQ-INT-063). The file travels
 * as bytes and its name as text — INT never builds or opens a file path (REQ-INT-015). Document
 * Access's upload refusals propagate through it unchanged.
 */
public interface DocumentAccessPort {

    /**
     * Hands one uploaded file over for the Check, under the Check's own service code and version.
     *
     * @return Document Access's receipt, unchanged
     */
    UploadReceipt handOver(UploadCommand command, String serviceCode, int versionNumber);

    /**
     * The uploaded documents of a Check, unchanged, in upload order, never their content; empty for
     * a Check with no upload (unknown or ended included).
     */
    List<UploadedDocumentSummary> listUploaded(Long checkId);
}
