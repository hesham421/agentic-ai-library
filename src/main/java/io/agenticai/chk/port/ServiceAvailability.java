package io.agenticai.chk.port;

/**
 * Whether new Checks may use a service code (RULE-CHK-001, REQ-CHK-005): the CHK port over the
 * service registry's availability read. Implemented by an integration adapter over REG's
 * published in-process interface, never in this module's own code.
 */
public interface ServiceAvailability {

    /**
     * @param serviceCode the service code exactly as received
     * @return {@code true} only for a code the registry holds whose service is available;
     *         {@code false} for an unknown or withdrawn code — nothing is thrown
     */
    boolean isServiceAvailable(String serviceCode);
}
