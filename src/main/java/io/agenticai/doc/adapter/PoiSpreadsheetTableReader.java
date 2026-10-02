package io.agenticai.doc.adapter;

import io.agenticai.doc.domain.DocumentFormat;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.SheetTable;
import io.agenticai.doc.port.SpreadsheetTableReader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The {@link SpreadsheetTableReader} over Apache POI (REQ-DOC-026, ADR-DOC-004): HSSF opens a
 * {@link DocumentFormat#XLS} workbook, XSSF a {@link DocumentFormat#XLSX} one — chosen by the
 * format the detector read from the content signature, never by a file name (REQ-DOC-024). The
 * only class of DOC that touches POI's workbook API (the format detector uses only its OLE2
 * directory parser).
 *
 * <p>Every sheet becomes one {@link SheetTable}: its rows from the first to the last row that
 * exists, its columns from the leftmost to the rightmost cell any of those rows holds, every
 * row of the same width, an absent cell {@code ""}. A cell's value is the text Excel displays —
 * {@link DataFormatter} applies the cell's number format, and a formula cell shows its evaluated
 * result; should POI be unable to evaluate one formula, that cell shows the formula text and the
 * sheet is still read.
 *
 * <p>Failure translation (REQ-DOC-029; A.4.9, E.1.5): a damaged or password-protected workbook —
 * an {@code IOException}, POI's {@code EncryptedDocumentException} or a runtime fault of the
 * parser on malformed input — is caught at this boundary only to become a recorded
 * READING_FAILED outcome with the exception message as detail, logged at debug level without
 * any content; nothing is swallowed and nothing external escapes. Stateless: every call parses
 * its own bytes and keeps nothing (guardrail G9).
 */
@Component
public class PoiSpreadsheetTableReader implements SpreadsheetTableReader {

    private static final Logger log = LoggerFactory.getLogger(PoiSpreadsheetTableReader.class);

    @Override
    public ReadOutcome<List<SheetTable>> read(byte[] workbook, DocumentFormat format) {
        Objects.requireNonNull(workbook, "workbook");
        Objects.requireNonNull(format, "format");
        if (format != DocumentFormat.XLS && format != DocumentFormat.XLSX) {
            throw new IllegalArgumentException("Not a spreadsheet format: " + format);
        }
        try (Workbook book = open(workbook, format)) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = book.getCreationHelper().createFormulaEvaluator();
            List<SheetTable> tables = new ArrayList<>(book.getNumberOfSheets());
            for (Sheet sheet : book) {
                tables.add(table(sheet, formatter, evaluator));
            }
            log.debug("DOC spreadsheet: {} sheet(s) read from a {} workbook", tables.size(), format);
            return new Read<>(List.copyOf(tables));
        } catch (Exception e) {
            // translation only: the reader's failure becomes the document's recorded outcome
            log.debug("DOC spreadsheet: table extraction failed: {}", describe(e), e);
            return new Unreadable<>(UnreadableReason.READING_FAILED,
                    "table extraction failed: " + describe(e));
        }
    }

    /** HSSF for {@code .xls}, XSSF for {@code .xlsx} — the format is the detector's, not a guess. */
    private static Workbook open(byte[] bytes, DocumentFormat format) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return switch (format) {
            case XLS -> new HSSFWorkbook(in);
            case XLSX -> new XSSFWorkbook(in);
            default -> throw new IllegalArgumentException("Not a spreadsheet format: " + format);
        };
    }

    private static SheetTable table(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        String name = sheet.getSheetName();
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return new SheetTable(name, List.of());
        }
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        int firstColumn = Integer.MAX_VALUE;
        int columnEnd = -1; // exclusive, as Row.getLastCellNum is
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null || row.getPhysicalNumberOfCells() == 0) {
                continue;
            }
            firstColumn = Math.min(firstColumn, row.getFirstCellNum());
            columnEnd = Math.max(columnEnd, row.getLastCellNum());
        }
        if (columnEnd < 0 || firstColumn >= columnEnd) {
            return new SheetTable(name, List.of());
        }
        List<List<String>> rows = new ArrayList<>(lastRow - firstRow + 1);
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            List<String> cells = new ArrayList<>(columnEnd - firstColumn);
            for (int c = firstColumn; c < columnEnd; c++) {
                Cell cell = row == null ? null : row.getCell(c);
                cells.add(cell == null ? "" : displayed(cell, formatter, evaluator));
            }
            rows.add(cells);
        }
        return new SheetTable(name, rows);
    }

    /** The cell as displayed; a formula POI cannot evaluate shows its formula text instead. */
    private static String displayed(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        try {
            return formatter.formatCellValue(cell, evaluator);
        } catch (RuntimeException e) {
            log.debug("DOC spreadsheet: formula at {} not evaluated ({}); its text is shown",
                    cell.getAddress(), describe(e), e);
            return formatter.formatCellValue(cell);
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + message;
    }
}
