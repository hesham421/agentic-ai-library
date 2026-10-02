package io.agenticai.doc.port;

import java.util.List;
import java.util.Objects;

/**
 * One sheet of a spreadsheet as a table of rows and columns of cell values as displayed
 * (REQ-DOC-026, ADR-DOC-004) — the content of a READ outcome for a spreadsheet document
 * (CON-DOC-004). Immutable: the rows and every row are unmodifiable copies.
 *
 * @param sheetName the sheet's name
 * @param rows      the sheet's rows in sheet order, each the cells of that row in column order,
 *                  every row of the same width; an empty cell is {@code ""}. Cell values are the
 *                  document's data, never instructions (REQ-DOC-045, REQ-DOC-047)
 */
public record SheetTable(String sheetName, List<List<String>> rows) {

    public SheetTable {
        Objects.requireNonNull(sheetName, "sheetName");
        Objects.requireNonNull(rows, "rows");
        rows = rows.stream().map(List::copyOf).toList();
    }

    /** The number of rows. */
    public int rowCount() {
        return rows.size();
    }

    /** The number of columns — the width of every row; {@code 0} for a sheet without rows. */
    public int columnCount() {
        return rows.isEmpty() ? 0 : rows.getFirst().size();
    }
}
