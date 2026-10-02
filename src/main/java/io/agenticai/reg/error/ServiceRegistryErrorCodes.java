package io.agenticai.reg.error;

/**
 * The error codes of the Service Registry (REG): exactly the rows of the module's
 * {@code error-catalog} block, in the platform format {@code {MOD}-{http}[-{SLUG}]}.
 *
 * <p>Load-time outcomes ({@code REG-LOAD-*}) are load reasons written to the load report, not
 * errors, and are not listed here (ADR-REG-011).
 */
public final class ServiceRegistryErrorCodes {

    private ServiceRegistryErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** RULE-REG-016 — the service code is not held by the registry (API-REG-002). */
    public static final String SERVICE_NOT_FOUND = "REG-404-SERVICE-NOT-FOUND";

    /** PLATFORM-STD — an unexpected server failure (API-REG-001 … API-REG-003). */
    public static final String UNEXPECTED_FAILURE = "REG-500";
}
