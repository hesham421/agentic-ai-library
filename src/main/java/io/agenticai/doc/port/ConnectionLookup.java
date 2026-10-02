package io.agenticai.doc.port;

import io.agenticai.reg.contract.ConnectionSettings;

import java.util.Optional;

/**
 * Outbound port of DOC to the Service Registry for the settings of the connection a document
 * source query names (REQ-DOC-012, REQ-DOC-014, REQ-DOC-052; CON-REG-011). The adapter behind it
 * is the only DOC class that asks the registry for a connection; the credential itself is never
 * here — the settings carry a reference the environment's secret store resolves.
 *
 * <p>An absent connection is not an error of the fetch: the query port, given no settings,
 * refuses the query under RULE-DOC-007 and every required document type becomes UNREADABLE /
 * SOURCE_QUERY_FAILED (ADR-DOC-007).
 */
public interface ConnectionLookup {

    /**
     * @param connectionName the connection the document source query names
     * @return the settings, as the registry supplies them; empty when no connection of that
     *         name is activated in this environment
     */
    Optional<ConnectionSettings> find(String connectionName);
}
