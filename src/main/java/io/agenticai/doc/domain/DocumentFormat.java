package io.agenticai.doc.domain;

/**
 * The closed list of document formats DOC reads (REQ-DOC-024 … REQ-DOC-028, ADR-DOC-004), each
 * recognised from the content signature by the {@link FormatDetector} — never from a file name.
 * Any other format is UNREADABLE / UNSUPPORTED_FORMAT (REQ-DOC-028).
 *
 * <ul>
 *   <li>{@link #PDF} — read by text extraction, or in the document-reading step when it has no
 *       extractable text (REQ-DOC-025, REQ-DOC-027)</li>
 *   <li>{@link #XLS} — a BIFF workbook in an OLE2 compound file, read by table extraction with
 *       HSSF (REQ-DOC-026)</li>
 *   <li>{@link #XLSX} — an Office Open XML workbook, read by table extraction with XSSF
 *       (REQ-DOC-026)</li>
 *   <li>{@link #IMAGE} — JPEG, PNG or TIFF, read in the document-reading step (REQ-DOC-027)</li>
 * </ul>
 */
public enum DocumentFormat {

    PDF,
    XLS,
    XLSX,
    IMAGE
}
