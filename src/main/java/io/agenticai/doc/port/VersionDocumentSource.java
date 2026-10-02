package io.agenticai.doc.port;

import java.util.Objects;

/**
 * The document source query of a service package version as DOC needs it (CON-REG-003): the SQL
 * text exactly as the service definition stores it and the name of the connection it runs on.
 * Named apart from {@link DocumentSourceQuery} — that record is one <em>run</em> of the query
 * with the request number bound; this one is the version's stored definition, as the registry
 * adapter behind {@link VersionLookup} maps it. Immutable; a copy kept only for the call.
 *
 * @param sqlText        the query text, unaltered (REQ-DOC-051)
 * @param connectionName the connection the query names
 */
public record VersionDocumentSource(String sqlText, String connectionName) {

    public VersionDocumentSource {
        Objects.requireNonNull(sqlText, "sqlText");
        Objects.requireNonNull(connectionName, "connectionName");
    }
}
