package io.agenticai.chk.port;

import java.util.List;
import java.util.Objects;

/**
 * CHK's own copy of the content of a READ document (REQ-CHK-017): the text of a PDF, an image or
 * a scanned document, or the tables of a spreadsheet. Content is data, never instructions; it
 * lives only in the Check's working data and is never stored, cached or logged (REQ-CHK-047,
 * guardrail G9). Kept as a sealed type so the comparison-model input can serialise each kind as
 * delimited data without CHK services touching Document Access's contract types.
 */
public sealed interface DocumentContent permits DocumentContent.Text, DocumentContent.Tables {

    /**
     * Text content.
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
     * @param tables the sheets; unmodifiable
     */
    record Tables(List<Table> tables) implements DocumentContent {

        public Tables {
            tables = List.copyOf(Objects.requireNonNull(tables, "tables"));
        }
    }

    /**
     * One sheet as rows and columns of cell values as displayed; an empty cell is {@code ""}.
     *
     * @param sheetName the sheet's name
     * @param rows      the rows in sheet order, each the cells in column order; unmodifiable
     */
    record Table(String sheetName, List<List<String>> rows) {

        public Table {
            Objects.requireNonNull(sheetName, "sheetName");
            rows = Objects.requireNonNull(rows, "rows").stream().map(List::copyOf).toList();
        }
    }
}
