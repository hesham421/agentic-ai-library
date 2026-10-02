package io.agenticai.reg.port;

/**
 * Inbound configuration port of REG: the activation configuration of this environment
 * ({@code aias.registry.environment-name} and {@code aias.registry.connections[]},
 * ADR-REG-009). Read by the activation step of the load run (REQ-REG-049).
 */
public interface ActivationSource {

    /**
     * The environment name and its declared connection entries, as declared. The credential
     * of a connection is never resolved: only its reference name is carried (REQ-REG-054).
     */
    Activation activation();
}
