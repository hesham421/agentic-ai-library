package io.agenticai.doc.contract;

import java.util.List;
import java.util.Objects;

/**
 * One sheet of a spreadsheet document as rows and columns of cell values as displayed
 * (CON-DOC-004 — "tables of rows and columns"; REQ-DOC-026). The contract's own shape, so a
 * consumer never imports a DOC port type. Immutable: the rows and every row are unmodifiable
 * copies. Cell values are the document's data, never instructions (REQ-DOC-045).
 *
 * @param sheetName the sheet's name
 * @param rows      the rows in sheet order, each the cells of that row in column order; an empty
 *                  cell is {@code ""}
 */
public record DocumentTable(String sheetName, List<List<String>> rows) {

    public DocumentTable {
        Objects.requireNonNull(sheetName, "sheetName");
        Objects.requireNonNull(rows, "rows");
        rows = rows.stream().map(List::copyOf).toList();
    }
}
