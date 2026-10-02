package io.agenticai.doc.service;

import io.agenticai.doc.domain.ReadOutcome;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * One document of a Check as the fetch procedure hands it to the reading step, whatever the fetch
 * mode (SVC-API fetchDocuments steps 2–4): the document type it carries and the way to obtain its
 * content. The content is a supplier so that a {@code path} document's bytes are read only when
 * the reading step asks for them and live only while it reads them; a {@code blob} or
 * {@code manual} document's outcome is already in hand and the supplier simply returns it.
 * Module-internal, a local value of one fetch — never stored (REQ-DOC-055).
 *
 * @param documentType the document type, as the host's type column or the employee gave it;
 *                     {@code null} when a host row's type column was NULL
 * @param content      the content outcome — the bytes, or the reason they could not be obtained
 */
public record FetchedDocument(String documentType, Supplier<ReadOutcome<byte[]>> content) {

    public FetchedDocument {
        Objects.requireNonNull(content, "content");
    }

    /** A document whose content outcome is already known. */
    public static FetchedDocument of(String documentType, ReadOutcome<byte[]> content) {
        Objects.requireNonNull(content, "content");
        return new FetchedDocument(documentType, () -> content);
    }
}
