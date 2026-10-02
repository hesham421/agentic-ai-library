package io.agenticai.reg.contract;

import java.util.Objects;

/**
 * RULE-REG-017 — the current version of a service names, in one of its queries, a connection the
 * environment does not register, so the version is not supplied to a Check (REQ-REG-053,
 * ADR-REG-009). An in-process refusal with the code {@link ServeRefusalCodes#CONNECTION_NOT_ACTIVATED}:
 * not an HTTP error (not in the error catalog) and never written to the load report
 * (ADR-REG-011). The message is the SRS English text of RULE-REG-017.
 */
public class ServiceConnectionNotActivatedException extends RuntimeException {

    /** The SRS message of RULE-REG-017 with its two placeholders. */
    static final String MESSAGE =
            "The service \"%s\" uses the connection \"%s\", which is not activated in this environment.";

    private final String serviceCode;
    private final String connectionName;

    /**
     * @param serviceCode    the canonical service code whose current version was requested
     * @param connectionName the connection name the version's query uses and the environment lacks
     */
    public ServiceConnectionNotActivatedException(String serviceCode, String connectionName) {
        super(MESSAGE.formatted(
                Objects.requireNonNull(serviceCode, "serviceCode"),
                Objects.requireNonNull(connectionName, "connectionName")));
        this.serviceCode = serviceCode;
        this.connectionName = connectionName;
    }

    /** {@link ServeRefusalCodes#CONNECTION_NOT_ACTIVATED}. */
    public String code() {
        return ServeRefusalCodes.CONNECTION_NOT_ACTIVATED;
    }

    public String serviceCode() {
        return serviceCode;
    }

    public String connectionName() {
        return connectionName;
    }
}
