package io.agenticai.chk.port;

import io.agenticai.reg.contract.ConnectionSettings;

import java.util.Optional;

/**
 * The settings of the connection a service query names (REQ-CHK-013, REQ-CHK-014): the CHK port
 * over the service registry's connection read. Implemented by an integration adapter over REG's
 * published in-process interface, never in this module's own code. The credential itself is never
 * part of the answer — only its reference.
 */
public interface ConnectionLookup {

    /**
     * @param connectionName the connection name as the query names it
     * @return the settings; empty when the connection is not registered in this environment (the
     *         query is then refused by RULE-CHK-002 and recorded as not read)
     */
    Optional<ConnectionSettings> find(String connectionName);
}
