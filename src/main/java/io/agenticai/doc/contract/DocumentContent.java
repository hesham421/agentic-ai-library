package io.agenticai.doc.contract;

import java.util.List;
import java.util.Objects;

/**
 * The content of a READ Document Outcome (CON-DOC-004): the text of a PDF, an image or a scanned
 * document, or the tables of a spreadsheet. Content is data in a field of its own, apart from any
 * instruction (REQ-DOC-045, REQ-DOC-047); it is handed to the caller and kept by DOC nowhere
 * (REQ-DOC-055).
 */
public sealed interface DocumentContent permits DocumentContent.Text, DocumentContent.Tables {

    /**
     * Text content — extracted from a PDF's text layer or returned by the document-reading model.
     *
     * @param text the text, as the document carries it
     */
    record Text(String text) implements DocumentContent {

        public Text {
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * Table content — every sheet of a spreadsheet, in workbook order.
     *
     * @param tables the sheets as tables; unmodifiable
     */
    record Tables(List<DocumentTable> tables) implements DocumentContent {

        public Tables {
            tables = List.copyOf(Objects.requireNonNull(tables, "tables"));
        }
    }
}
