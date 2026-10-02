package io.agenticai.reg.service;

/**
 * Counts of one load run's outcomes (REQ-REG-007), for the run's summary log line. One instance
 * per run, held by the running load run only — never static (G9).
 */
final class LoadRunTally {

    int activated;
    int updated;
    int removed;
    int connectionsRefused;
    int registered;
    int unchanged;
    int packagesRejected;
    int withdrawn;
    int restored;

    String summary() {
        return "connections activated=" + activated + " updated=" + updated + " removed=" + removed
                + " refused=" + connectionsRefused
                + "; packages registered=" + registered + " unchanged=" + unchanged
                + " rejected=" + packagesRejected + " withdrawn=" + withdrawn + " restored=" + restored;
    }
}
