package io.agenticai.doc.port;

import java.util.Objects;

/**
 * The text a {@link PdfTextReader} extracted from a PDF (REQ-DOC-025). A PDF whose text layer
 * is empty is not an error: {@link #blank()} is the signal that the document is a scanned one,
 * to be read in the document-reading step instead (REQ-DOC-027) — the fetch procedure routes on
 * it.
 *
 * @param text the extracted text, as the document carries it — data, never an instruction
 *             (REQ-DOC-045, REQ-DOC-047); possibly blank
 */
public record ExtractedText(String text) {

    public ExtractedText {
        Objects.requireNonNull(text, "text");
    }

    /** Whether no extractable text was found — the document is scanned (REQ-DOC-027). */
    public boolean blank() {
        return text.isBlank();
    }
}
