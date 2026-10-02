package io.agenticai.doc.port;

import io.agenticai.doc.domain.DocumentFormat;
import io.agenticai.doc.domain.ReadOutcome;

import java.util.List;

/**
 * Document reader port for spreadsheets: table extraction of every sheet, rows × columns of
 * cell values as displayed (REQ-DOC-026, ADR-DOC-004). The adapter is replaceable; the one DOC
 * ships uses Apache POI — HSSF for {@code .xls}, XSSF for {@code .xlsx}.
 *
 * <p>A reader holds no state and parses only the bytes of each call (guardrail G9). A damaged
 * or password-protected workbook is a recorded {@link ReadOutcome.Unreadable} with reason
 * READING_FAILED and the reader's exception message as detail (REQ-DOC-029) — never thrown.
 */
public interface SpreadsheetTableReader {

    /**
     * @param workbook the document's bytes, already recognised by the format detector
     * @param format   {@link DocumentFormat#XLS} or {@link DocumentFormat#XLSX}, as detected from
     *                 the content signature — never from a file name (REQ-DOC-024)
     * @return every sheet as a table, in workbook order (unmodifiable), or UNREADABLE /
     *         READING_FAILED; never throws for a document
     * @throws IllegalArgumentException when {@code format} is not a spreadsheet format — API
     *                                  misuse by the caller, not a document failure
     */
    ReadOutcome<List<SheetTable>> read(byte[] workbook, DocumentFormat format);
}
