package io.agenticai.reg.contract;

import java.util.Objects;

/**
 * Where a version's documents come from (CON-REG-007, CON-REG-009; REQ-REG-039): the query of the
 * same version that lists them and the columns of its result. Present for fetch modes
 * {@code path} and {@code blob}; a {@code manual} version has none.
 *
 * @param documentSourceQueryName the query that lists the documents
 * @param documentTypeColumn      the column holding the document type
 * @param documentPathColumn      the column holding the document location — fetch mode
 *                                {@code path}; {@code null} otherwise unless declared
 * @param documentContentColumn   the column holding the document content — fetch mode
 *                                {@code blob}; {@code null} otherwise unless declared
 */
public record DocumentSource(String documentSourceQueryName,
                             String documentTypeColumn,
                             String documentPathColumn,
                             String documentContentColumn) {

    public DocumentSource {
        Objects.requireNonNull(documentSourceQueryName, "documentSourceQueryName");
        Objects.requireNonNull(documentTypeColumn, "documentTypeColumn");
    }
}
