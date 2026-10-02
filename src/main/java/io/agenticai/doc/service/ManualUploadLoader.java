package io.agenticai.doc.service;

import io.agenticai.platform.config.CheckLimitsProperties;
import io.agenticai.doc.domain.FileSize;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.entity.UploadedDocument;
import io.agenticai.doc.repository.UploadedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The {@code manual} branch of the fetch (SVC-API fetchDocuments step 4; REQ-DOC-018,
 * REQ-DOC-019, REQ-DOC-056): the Check's own Uploaded Documents, content included, read in one
 * short read-only transaction and handed out as plain values — so that the reading step, the
 * host file port and the document-reading model never run while a database transaction is open.
 * No query, no host file, no JDBC: only DOC's own table, filtered by CHECK_ID (RULE-DOC-008).
 *
 * <p>An oversized upload is UNREADABLE / TOO_LARGE by construction (REQ-DOC-044, RULE-DOC-005):
 * its content was never kept. The content of every other upload is materialised here, inside the
 * transaction, and belongs to the fetching call alone (REQ-DOC-055).
 */
@Service
@Transactional(readOnly = true)
public class ManualUploadLoader {

    private static final Logger log = LoggerFactory.getLogger(ManualUploadLoader.class);

    private final UploadedDocumentRepository uploads;
    private final long maxFileSizeBytes;

    public ManualUploadLoader(UploadedDocumentRepository uploads, CheckLimitsProperties limits) {
        this.uploads = Objects.requireNonNull(uploads, "uploads");
        this.maxFileSizeBytes = Objects.requireNonNull(limits, "limits").maxFileSize().toBytes();
    }

    /**
     * The Uploaded Documents carrying {@code checkId}, in upload order, each with its content
     * outcome.
     */
    public List<FetchedDocument> load(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        List<UploadedDocument> rows = uploads.findByCheckIdOrderByCreatedAtAsc(checkId);
        log.debug("DOC manual fetch checkId={} uploads={}", checkId, rows.size());
        List<FetchedDocument> fetched = new ArrayList<>(rows.size());
        for (UploadedDocument upload : rows) {
            fetched.add(FetchedDocument.of(upload.getDocumentType(), contentOf(upload)));
        }
        return List.copyOf(fetched);
    }

    private ReadOutcome<byte[]> contentOf(UploadedDocument upload) {
        if (upload.isOversized()) {
            return new Unreadable<>(UnreadableReason.TOO_LARGE, FileSize.tooLargeDetail(
                    "the uploaded file \"" + upload.getFileName() + "\"", upload.getFileSize(), maxFileSizeBytes));
        }
        byte[] content = upload.getContent();
        if (content == null) {
            // excluded by CHK_DOC_UPLOADED_DOC_CONTENT; reported rather than dropped should it ever occur (G6)
            return new Unreadable<>(UnreadableReason.NOT_FOUND,
                    "the uploaded file \"" + upload.getFileName() + "\" holds no content");
        }
        return new Read<>(content);
    }
}
