package io.agenticai.reg.service;

/**
 * How a start-up load run ended (ADR-REG-015).
 */
public enum LoadRunOutcome {

    /** The lock was granted; the run completed and committed its load report. */
    COMPLETED,

    /**
     * The load lock was not granted within {@code aias.registry.load-lock-timeout}: nothing was
     * written and the instance serves the registry as stored (REQ-REG-068).
     */
    LOCK_NOT_GRANTED
}
