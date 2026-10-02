package io.agenticai.doc.service;

import io.agenticai.doc.contract.DocumentContent;
import io.agenticai.doc.contract.DocumentOutcome;
import io.agenticai.doc.contract.DocumentTable;
import io.agenticai.doc.domain.DetectedFormat;
import io.agenticai.doc.domain.DocumentFormat;
import io.agenticai.doc.domain.FormatDetector;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.port.DocumentReadingModelPort;
import io.agenticai.doc.port.ExtractedText;
import io.agenticai.doc.port.PdfTextReader;
import io.agenticai.doc.port.SheetTable;
import io.agenticai.doc.port.SpreadsheetTableReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The reading of one fetched document (SVC-API fetchDocuments step 5): the format from the
 * content signature, then the reader it chooses — text extraction for a PDF, the
 * document-reading model for a scanned PDF or an image, table extraction for a spreadsheet
 * (REQ-DOC-024 … REQ-DOC-029) — into exactly one Document Outcome: READ with its content, or
 * UNREADABLE with one reason and a detail (REQ-DOC-034, REQ-DOC-036, REQ-DOC-038).
 *
 * <p>Each document stands alone in a try/catch of its own: a failure no adapter translated is
 * UNREADABLE / READING_FAILED and never stops the other documents (REQ-DOC-037). Every step
 * checks the Check's deadline first; once passed, the document is UNREADABLE / OUT_OF_TIME
 * (REQ-DOC-040). Nothing here runs inside a database transaction, so no model or host call is
 * made while one is open; the bytes are local to the call and referenced by no field
 * (REQ-DOC-055, G9). Content is never logged.
 */
@Service
public class DocumentReadStep {

    private static final Logger log = LoggerFactory.getLogger(DocumentReadStep.class);

    private final FormatDetector formats = new FormatDetector();
    private final PdfTextReader pdfText;
    private final SpreadsheetTableReader spreadsheets;
    private final DocumentReadingModelPort readingModel;

    public DocumentReadStep(PdfTextReader pdfText,
                            SpreadsheetTableReader spreadsheets,
                            DocumentReadingModelPort readingModel) {
        this.pdfText = Objects.requireNonNull(pdfText, "pdfText");
        this.spreadsheets = Objects.requireNonNull(spreadsheets, "spreadsheets");
        this.readingModel = Objects.requireNonNull(readingModel, "readingModel");
    }

    /**
     * One outcome for {@code document}, whatever happens to it.
     *
     * @param document   the fetched document
     * @param sourceMode the version's fetch mode, as its stored value (CON-DOC-002)
     * @param deadline   the Check's deadline (REQ-DOC-040)
     */
    public DocumentOutcome read(FetchedDocument document, String sourceMode, Instant deadline) {
        Objects.requireNonNull(document, "document");
        String documentType = document.documentType();
        try {
            return switch (document.content().get()) {
                case Unreadable<byte[]> notObtained -> unreadable(documentType, sourceMode, notObtained);
                case Read<byte[]> obtained -> readBytes(documentType, sourceMode, obtained.value(), deadline);
            };
        } catch (RuntimeException e) {
            log.debug("DOC document reading failed unexpectedly documentType={}", documentType, e);
            return unreadable(documentType, sourceMode, ReadFailures.readingFailed(e));
        }
    }

    private DocumentOutcome readBytes(String documentType, String sourceMode, byte[] bytes, Instant deadline) {
        if (ReadFailures.passed(deadline)) {
            return outOfTime(documentType, sourceMode, deadline);
        }
        return switch (formats.detect(bytes)) {
            case Unreadable<DetectedFormat> unsupported -> unreadable(documentType, sourceMode, unsupported);
            case Read<DetectedFormat> detected -> readAs(documentType, sourceMode, bytes, detected.value(), deadline);
        };
    }

    private DocumentOutcome readAs(String documentType, String sourceMode, byte[] bytes,
                                   DetectedFormat detected, Instant deadline) {
        log.debug("DOC reading documentType={} format={}", documentType, detected.format());
        return switch (detected.format()) {
            case PDF -> readPdf(documentType, sourceMode, bytes, detected.mediaType(), deadline);
            case XLS, XLSX -> readSpreadsheet(documentType, sourceMode, bytes, detected.format(), deadline);
            case IMAGE -> readWithModel(documentType, sourceMode, bytes, detected.mediaType(), deadline);
        };
    }

    /** REQ-DOC-025, REQ-DOC-027: the text layer, or the document-reading model when it is blank. */
    private DocumentOutcome readPdf(String documentType, String sourceMode, byte[] bytes,
                                    String mediaType, Instant deadline) {
        if (ReadFailures.passed(deadline)) {
            return outOfTime(documentType, sourceMode, deadline);
        }
        return switch (pdfText.read(bytes)) {
            case Unreadable<ExtractedText> failed -> unreadable(documentType, sourceMode, failed);
            case Read<ExtractedText> extracted -> extracted.value().blank()
                    ? readWithModel(documentType, sourceMode, bytes, mediaType, deadline)
                    : DocumentOutcome.read(documentType, sourceMode, new DocumentContent.Text(extracted.value().text()));
        };
    }

    /** REQ-DOC-026: every sheet as a table. */
    private DocumentOutcome readSpreadsheet(String documentType, String sourceMode, byte[] bytes,
                                            DocumentFormat format, Instant deadline) {
        if (ReadFailures.passed(deadline)) {
            return outOfTime(documentType, sourceMode, deadline);
        }
        return switch (spreadsheets.read(bytes, format)) {
            case Unreadable<List<SheetTable>> failed -> unreadable(documentType, sourceMode, failed);
            case Read<List<SheetTable>> sheets -> DocumentOutcome.read(documentType, sourceMode,
                    new DocumentContent.Tables(sheets.value().stream()
                            .map(sheet -> new DocumentTable(sheet.sheetName(), sheet.rows()))
                            .toList()));
        };
    }

    /** REQ-DOC-027: the document-reading model, one document per call (REQ-DOC-057). */
    private DocumentOutcome readWithModel(String documentType, String sourceMode, byte[] bytes,
                                          String mediaType, Instant deadline) {
        if (ReadFailures.passed(deadline)) {
            return outOfTime(documentType, sourceMode, deadline);
        }
        return switch (readingModel.read(bytes, mediaType, deadline)) {
            case Unreadable<String> failed -> unreadable(documentType, sourceMode, failed);
            case Read<String> text -> DocumentOutcome.read(documentType, sourceMode, new DocumentContent.Text(text.value()));
        };
    }

    private static DocumentOutcome outOfTime(String documentType, String sourceMode, Instant deadline) {
        return unreadable(documentType, sourceMode, ReadFailures.outOfTime(deadline));
    }

    private static DocumentOutcome unreadable(String documentType, String sourceMode, Unreadable<?> failure) {
        return DocumentOutcome.unreadable(documentType, sourceMode, failure.reason().storedValue(), failure.detail());
    }
}
