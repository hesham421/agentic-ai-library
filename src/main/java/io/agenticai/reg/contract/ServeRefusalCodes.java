package io.agenticai.reg.contract;

/**
 * The in-process refusal codes of the Service Registry — the {@code REG-SERVE-*} namespace marks
 * serve-time refusals of the in-process interface that are neither HTTP errors (they are not in
 * the module's error catalog, so they are not in {@code ServiceRegistryErrorCodes}) nor load
 * reasons (they are never written to the load report). ADR-REG-011.
 */
public final class ServeRefusalCodes {

    private ServeRefusalCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /**
     * RULE-REG-017 — the current version of a service names a connection the environment does not
     * register ({@link ServiceConnectionNotActivatedException}).
     */
    public static final String CONNECTION_NOT_ACTIVATED = "REG-SERVE-CONNECTION-NOT-ACTIVATED";
}
