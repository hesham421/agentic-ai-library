package io.agenticai.rpt.domain;

/**
 * The fixed limits of the Report Store's reads. They are constants of the service, not
 * configuration: {@code aias.reports.list-limit} is deliberately NOT a property (RPT CORE R1,
 * ADR-RPT-008).
 */
public final class ReportLimits {

    private ReportLimits() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /**
     * The most Checks one listing of a request's Checks returns, newest first; the listing also
     * carries the total so the cut is visible (REQ-RPT-031, ADR-RPT-008, API-RPT-002).
     */
    public static final int LIST_LIMIT = 100;
}
