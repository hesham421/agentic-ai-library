package io.agenticai.reg.contract;

import java.util.Objects;

/**
 * CON-REG-011 "not found" — no connection of that name is activated in this environment
 * (REQ-REG-048). An in-process refusal; the contract states no code for it, so none is carried
 * (recorded as an {@code api_doc_gaps} row of this unit). It is not an HTTP error and never a load
 * reason.
 */
public class ConnectionNotFoundException extends RuntimeException {

    private final String connectionName;

    /**
     * @param connectionName the connection name that was requested
     */
    public ConnectionNotFoundException(String connectionName) {
        super("The connection \"" + Objects.requireNonNull(connectionName, "connectionName")
                + "\" is not activated in this environment.");
        this.connectionName = connectionName;
    }

    public String connectionName() {
        return connectionName;
    }
}
