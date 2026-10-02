package io.agenticai.doc.port;

import io.agenticai.doc.domain.FetchMode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The document settings of one service package version, as DOC reads them from the registry
 * through {@link VersionLookup} (REQ-DOC-002): the fetch mode (CON-REG-009), the document source
 * — its query (CON-REG-003) and the columns of its result — and the required document types
 * (CON-REG-004). DOC's own immutable shape: no registry type beyond its published contract is
 * held, no foreign key exists and nothing is kept beyond the call.
 *
 * @param fetchMode               how the version's documents are obtained (CON-DOC-002)
 * @param inputName               the name of the only bind parameter of every query of the version
 * @param documentSourceQueryName the query that lists the documents; {@code null} for
 *                                {@code manual}
 * @param documentTypeColumn      the column holding each row's document type (REQ-DOC-006);
 *                                {@code null} for {@code manual}
 * @param documentPathColumn      the column holding each row's path — {@code path} mode;
 *                                {@code null} otherwise unless declared
 * @param documentContentColumn   the column holding each row's content — {@code blob} mode;
 *                                {@code null} otherwise unless declared
 * @param documentSourceQuery     the document source query's text and connection; {@code null}
 *                                for {@code manual}
 * @param requiredDocumentTypes   the required document types, exactly as stored and in declared
 *                                order; unmodifiable, empty when none
 */
public record VersionDocumentSettings(FetchMode fetchMode,
                                      String inputName,
                                      String documentSourceQueryName,
                                      String documentTypeColumn,
                                      String documentPathColumn,
                                      String documentContentColumn,
                                      VersionDocumentSource documentSourceQuery,
                                      Set<String> requiredDocumentTypes) {

    public VersionDocumentSettings {
        Objects.requireNonNull(fetchMode, "fetchMode");
        Objects.requireNonNull(inputName, "inputName");
        requiredDocumentTypes = requiredDocumentTypes == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(requiredDocumentTypes));
    }
}
