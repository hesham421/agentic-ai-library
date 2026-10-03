package io.agenticai.doc.adapter;

import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.ExtractedText;
import io.agenticai.doc.port.PdfTextReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * The {@link PdfTextReader} over Apache PDFBox (REQ-DOC-025): the document is loaded from its
 * bytes and its text layer extracted with {@link PDFTextStripper}. PDFBox is touched only in
 * DOC's adapter package: here, and in {@link PdfPageRenderer}, which renders a PDF whose text
 * layer is blank to page images for the document-reading model.
 *
 * <p>Failure translation (REQ-DOC-029; A.4.9, E.1.5): a damaged, encrypted or
 * password-protected PDF — an {@code IOException} (PDFBox's {@code InvalidPasswordException}
 * among them) or a runtime fault of the parser on malformed input — is caught at this boundary
 * only to become a recorded READING_FAILED outcome with the exception message as detail, logged
 * at debug level without any content; nothing is swallowed and nothing external escapes.
 *
 * <p>A blank text layer is not a failure: the text is returned and {@link ExtractedText#blank()}
 * routes the document to the document-reading step (REQ-DOC-027). Stateless: every call parses
 * its own bytes and keeps nothing (guardrail G9).
 */
@Component
public class PdfBoxTextReader implements PdfTextReader {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxTextReader.class);

    @Override
    public ReadOutcome<ExtractedText> read(byte[] pdf) {
        Objects.requireNonNull(pdf, "pdf");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            log.debug("DOC pdf text: {} page(s), {} character(s) extracted",
                    document.getNumberOfPages(), text.length());
            return new Read<>(new ExtractedText(text));
        } catch (Exception e) {
            // translation only: the reader's failure becomes the document's recorded outcome
            log.debug("DOC pdf text: extraction failed: {}", describe(e), e);
            return new Unreadable<>(UnreadableReason.READING_FAILED,
                    "text extraction failed: " + describe(e));
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + message;
    }
}
