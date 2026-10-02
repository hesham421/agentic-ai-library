package io.agenticai.doc.service;

import io.agenticai.doc.contract.DocumentAccess;
import io.agenticai.doc.contract.DocumentOutcome;
import io.agenticai.doc.contract.EndCheckResult;
import io.agenticai.doc.contract.UploadReceipt;
import io.agenticai.doc.contract.UploadedDocumentSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The one {@link DocumentAccess} bean — the in-process interface INT and CHK inject
 * (contract-doc.md, CON-DOC-003 … CON-DOC-006; ADR-DOC-011). A facade and nothing more: each
 * operation is delegated, as is, to the service that owns its procedure, so that each keeps its
 * own transaction boundary — CON-DOC-003 to {@link UploadHandoverService}, CON-DOC-004 to
 * {@link DocumentFetchService}, CON-DOC-005 to {@link CheckEndService}, CON-DOC-006 to
 * {@link UploadedDocumentQueryService}. No authorization: caller authentication is deferred
 * (raw-idea A2).
 */
@Service
public class DocumentAccessService implements DocumentAccess {

    private final UploadHandoverService handover;
    private final DocumentFetchService fetch;
    private final CheckEndService checkEnd;
    private final UploadedDocumentQueryService query;

    public DocumentAccessService(UploadHandoverService handover,
                                 DocumentFetchService fetch,
                                 CheckEndService checkEnd,
                                 UploadedDocumentQueryService query) {
        this.handover = Objects.requireNonNull(handover, "handover");
        this.fetch = Objects.requireNonNull(fetch, "fetch");
        this.checkEnd = Objects.requireNonNull(checkEnd, "checkEnd");
        this.query = Objects.requireNonNull(query, "query");
    }

    @Override
    public UploadReceipt handOverUpload(Long checkId, String serviceCode, int versionNumber,
                                        String documentType, String fileName, byte[] bytes) {
        return handover.handOverUpload(checkId, serviceCode, versionNumber, documentType, fileName, bytes);
    }

    @Override
    public List<DocumentOutcome> fetchDocuments(Long checkId, String requestNumber, String serviceCode,
                                                int versionNumber, Instant deadline) {
        return fetch.fetchDocuments(checkId, requestNumber, serviceCode, versionNumber, deadline);
    }

    @Override
    public EndCheckResult endCheck(Long checkId) {
        return checkEnd.endCheck(checkId);
    }

    @Override
    public List<UploadedDocumentSummary> listUploadedDocuments(Long checkId) {
        return query.listUploadedDocuments(checkId);
    }
}
