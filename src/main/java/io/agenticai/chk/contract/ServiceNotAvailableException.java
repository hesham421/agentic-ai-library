package io.agenticai.chk.contract;

import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.error.CheckEngineTexts;

import java.util.Objects;

/**
 * RULE-CHK-001 — the service code is not held by the service registry or the service is not
 * available; nothing is created (REQ-CHK-005, CON-CHK-004). In-process code
 * {@value CheckRejectionCodes#SERVICE_NOT_AVAILABLE} (ADR-CHK-018).
 */
public class ServiceNotAvailableException extends CheckEngineException {

    private final String serviceCode;

    /** @param serviceCode the service code exactly as received */
    public ServiceNotAvailableException(String serviceCode) {
        this(serviceCode, null);
    }

    /**
     * @param serviceCode the service code exactly as received
     * @param cause       the registry's own refusal, kept as the cause; may be {@code null}
     */
    public ServiceNotAvailableException(String serviceCode, Throwable cause) {
        super(CheckRejectionCodes.SERVICE_NOT_AVAILABLE,
                CheckEngineTexts.english(CheckRejectionCodes.SERVICE_NOT_AVAILABLE,
                        Objects.requireNonNull(serviceCode, "serviceCode")),
                cause);
        this.serviceCode = serviceCode;
    }

    public String serviceCode() {
        return serviceCode;
    }
}
