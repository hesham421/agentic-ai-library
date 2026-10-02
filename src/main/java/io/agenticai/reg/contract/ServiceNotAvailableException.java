package io.agenticai.reg.contract;

import io.agenticai.reg.error.ServiceRegistryErrorCodes;
import io.agenticai.reg.error.ServiceRegistryException;

import java.util.Objects;

/**
 * RULE-REG-016 — the service code is not held by the registry or its Service Package is withdrawn,
 * so no service package is supplied (REQ-REG-012). Raised by the in-process interface; the same
 * rule answers the HTTP read with {@code REG-404-SERVICE-NOT-FOUND}, so this exception carries that
 * catalog code and its English message («The service "{serviceCode}" is not available.»).
 */
public class ServiceNotAvailableException extends ServiceRegistryException {

    private final String serviceCode;

    /**
     * @param serviceCode the canonical service code that was requested
     */
    public ServiceNotAvailableException(String serviceCode) {
        super(ServiceRegistryErrorCodes.SERVICE_NOT_FOUND, Objects.requireNonNull(serviceCode, "serviceCode"));
        this.serviceCode = serviceCode;
    }

    /** The canonical service code that was requested. */
    public String serviceCode() {
        return serviceCode;
    }
}
