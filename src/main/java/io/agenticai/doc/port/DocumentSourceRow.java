package io.agenticai.doc.port;

import io.agenticai.doc.domain.ReadOutcome;

import java.util.Objects;

/**
 * One row of a document source query's result, as the {@link DocumentSourceQueryPort} returns
 * it: the document type the host gave the row (REQ-DOC-006) and, by fetch mode, either where
 * the document is or what it holds.
 *
 * <ul>
 *   <li>{@code path} mode — {@link #location} is the path column's value and {@link #content} is
 *       {@code null}: the file is read afterwards by the host file port (REQ-DOC-005);</li>
 *   <li>{@code blob} mode — {@link #content} is the content column's outcome and
 *       {@link #location} is {@code null}: the bytes when the column was within the limit, else
 *       the recorded reason (NOT_FOUND for a NULL or empty column, REQ-DOC-015; TOO_LARGE for an
 *       oversized one, REQ-DOC-042) — a row is never dropped for its content (G6).</li>
 * </ul>
 *
 * @param documentType the document type column's value, as the host returned it; {@code null}
 *                     when the column was NULL — the fetch procedure decides what such a row is
 * @param location     the path column's value, {@code path} mode; {@code null} otherwise, and
 *                     {@code null} when the column was NULL
 * @param content      the content column's outcome, {@code blob} mode; {@code null} otherwise
 */
public record DocumentSourceRow(String documentType, String location, ReadOutcome<byte[]> content) {

    /** A {@code path} row: the type and the path, no content. */
    public static DocumentSourceRow pathRow(String documentType, String location) {
        return new DocumentSourceRow(documentType, location, null);
    }

    /** A {@code blob} row: the type and the content column's outcome. */
    public static DocumentSourceRow blobRow(String documentType, ReadOutcome<byte[]> content) {
        return new DocumentSourceRow(documentType, null, Objects.requireNonNull(content, "content"));
    }
}
