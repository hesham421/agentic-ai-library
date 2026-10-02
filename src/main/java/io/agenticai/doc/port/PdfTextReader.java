package io.agenticai.doc.port;

import io.agenticai.doc.domain.ReadOutcome;

/**
 * Document reader port for PDF: text extraction (REQ-DOC-025). The adapter is replaceable; the
 * one DOC ships uses Apache PDFBox.
 *
 * <p>A reader holds no state and parses only the bytes of each call (guardrail G9). A damaged,
 * encrypted or password-protected PDF is a recorded {@link ReadOutcome.Unreadable} with reason
 * READING_FAILED and the reader's exception message as detail (REQ-DOC-029) — never thrown. A
 * blank text layer is a {@link ReadOutcome.Read} whose {@link ExtractedText#blank()} is true
 * (REQ-DOC-027).
 */
public interface PdfTextReader {

    /**
     * @param pdf the document's bytes, already recognised as PDF by the format detector
     * @return the extracted text, or UNREADABLE / READING_FAILED; never throws
     */
    ReadOutcome<ExtractedText> read(byte[] pdf);
}
