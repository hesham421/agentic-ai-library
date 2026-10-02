package io.agenticai.chk.adapter;

import io.agenticai.chk.port.DocumentContent;
import io.agenticai.chk.port.DocumentFetchFailedException;
import io.agenticai.chk.port.DocumentOutcome;
import io.agenticai.chk.port.DocumentPort;
import io.agenticai.doc.contract.DocumentAccess;
import io.agenticai.doc.contract.DocumentTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The {@link DocumentPort} over Document Access's published in-process interface
 * {@link DocumentAccess} (same deployable, injected by type): the only CHK class that calls it,
 * and so the only way CHK gets documents (REQ-CHK-017, REQ-CHK-018).
 *
 * <ul>
 *   <li>{@link #fetch} — one call to {@code fetchDocuments} with the Check's identifier, request
 *       number, service code, version number and deadline; each outcome is mapped one to one
 *       into CHK's own {@link DocumentOutcome}, its content into CHK's {@link DocumentContent}.
 *       A failure of the call — DOC's {@code ServiceVersionNotFoundException} or any other
 *       runtime failure — becomes {@link DocumentFetchFailedException} with DOC's message as the
 *       failure text and DOC's exception as cause (REQ-CHK-023; E.1.4/E.1.5).</li>
 *   <li>{@link #endCheck} — one call to {@code endCheck}; on failure the notice is sent once more,
 *       and a second failure is logged with the Check identifier and not rethrown, so the Check
 *       keeps its ending (REQ-CHK-062). This is the documented exception to E.5.3: the SRS
 *       prescribes log-and-continue for this path.</li>
 * </ul>
 *
 * <p>Stateless: outcomes are mapped and handed back inside the call — no field, no cache (G9).
 * Nothing logged here carries document content.
 */
@Component
public class DocumentAccessAdapter implements DocumentPort {

    private static final Logger log = LoggerFactory.getLogger(DocumentAccessAdapter.class);

    private final DocumentAccess documentAccess;

    public DocumentAccessAdapter(DocumentAccess documentAccess) {
        this.documentAccess = Objects.requireNonNull(documentAccess, "documentAccess");
    }

    @Override
    public List<DocumentOutcome> fetch(Long checkId,
                                       String requestNumber,
                                       String serviceCode,
                                       int versionNumber,
                                       Instant deadline) {
        List<io.agenticai.doc.contract.DocumentOutcome> outcomes;
        try {
            outcomes = documentAccess.fetchDocuments(checkId, requestNumber, serviceCode, versionNumber, deadline);
        } catch (RuntimeException failure) {
            throw new DocumentFetchFailedException(failureText(failure), failure);
        }
        return outcomes.stream().map(DocumentAccessAdapter::toOutcome).toList();
    }

    @Override
    public void endCheck(Long checkId) {
        try {
            documentAccess.endCheck(checkId);
        } catch (RuntimeException first) {
            try {
                documentAccess.endCheck(checkId);
            } catch (RuntimeException second) {
                // REQ-CHK-062: the Check keeps its ending; the second failure is recorded in the log.
                log.warn("The end-of-Check notice for Check {} failed twice; the Check keeps its ending",
                        checkId, second);
            }
        }
    }

    private static String failureText(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static DocumentOutcome toOutcome(io.agenticai.doc.contract.DocumentOutcome outcome) {
        return new DocumentOutcome(
                outcome.documentType(),
                outcome.sourceMode(),
                outcome.readStatus(),
                outcome.reason(),
                outcome.detail(),
                toContent(outcome.content()));
    }

    private static DocumentContent toContent(io.agenticai.doc.contract.DocumentContent content) {
        return switch (content) {
            case null -> null;
            case io.agenticai.doc.contract.DocumentContent.Text text -> new DocumentContent.Text(text.text());
            case io.agenticai.doc.contract.DocumentContent.Tables tables -> new DocumentContent.Tables(
                    tables.tables().stream().map(DocumentAccessAdapter::toTable).toList());
        };
    }

    private static DocumentContent.Table toTable(DocumentTable table) {
        return new DocumentContent.Table(table.sheetName(), table.rows());
    }
}
