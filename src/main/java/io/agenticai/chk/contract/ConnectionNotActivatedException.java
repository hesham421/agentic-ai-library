package io.agenticai.chk.contract;

import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.error.CheckEngineTexts;

import java.util.Objects;

/**
 * REQ-CHK-006 — the service registry reports that a connection named by the current service
 * package version is not activated in this environment; nothing is created (CON-CHK-004).
 * In-process code {@value CheckRejectionCodes#CONNECTION_NOT_ACTIVATED} (ADR-CHK-018).
 */
public class ConnectionNotActivatedException extends CheckEngineException {

    private final String serviceCode;
    private final String connectionName;

    /**
     * @param serviceCode    the service code exactly as received
     * @param connectionName the connection that is not activated
     * @param cause          the registry's own refusal, kept as the cause; may be {@code null}
     */
    public ConnectionNotActivatedException(String serviceCode, String connectionName, Throwable cause) {
        super(CheckRejectionCodes.CONNECTION_NOT_ACTIVATED,
                CheckEngineTexts.english(CheckRejectionCodes.CONNECTION_NOT_ACTIVATED,
                        Objects.requireNonNull(serviceCode, "serviceCode"),
                        Objects.requireNonNull(connectionName, "connectionName")),
                cause);
        this.serviceCode = serviceCode;
        this.connectionName = connectionName;
    }

    public String serviceCode() {
        return serviceCode;
    }

    public String connectionName() {
        return connectionName;
    }
}
