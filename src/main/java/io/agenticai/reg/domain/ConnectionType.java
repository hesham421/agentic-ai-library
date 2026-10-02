package io.agenticai.reg.domain;

import java.util.Optional;

/**
 * The kind of a registered connection (ENT-REG-005.connectionType). Closed: the stored values
 * are exactly those of {@code CHK_REG_CONNECTION_CONNECTION_TYPE} — {@code mcp | jdbc}.
 */
public enum ConnectionType {

    MCP("mcp"),
    JDBC("jdbc");

    private final String storedValue;

    ConnectionType(String storedValue) {
        this.storedValue = storedValue;
    }

    /** The value as stored in {@code CONNECTION_TYPE}. */
    public String storedValue() {
        return storedValue;
    }

    /**
     * The connection type of a stored value; empty when the value is not one of the closed set
     * (an unknown type is a REJECTED load reason, RULE-REG-014 — never an HTTP error).
     */
    public static Optional<ConnectionType> fromStored(String storedValue) {
        for (ConnectionType type : values()) {
            if (type.storedValue.equals(storedValue)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
