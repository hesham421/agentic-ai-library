package io.agenticai.rpt.contract;

import java.util.List;
import java.util.Objects;

/**
 * The Checks of one service code and request number (CON-RPT-004; API-RPT-002 response schema
 * {@code ChecksOfRequest}, no envelope): at most {@code ReportLimits.LIST_LIMIT} (100), newest
 * first, with the total so the cut is visible (REQ-RPT-028, REQ-RPT-031).
 *
 * @param total  every Check of the service code and request number
 * @param checks the newest Checks, unmodifiable
 */
public record ChecksOfRequest(long total, List<CheckSummary> checks) {

    public ChecksOfRequest {
        checks = List.copyOf(Objects.requireNonNull(checks, "checks"));
    }
}
