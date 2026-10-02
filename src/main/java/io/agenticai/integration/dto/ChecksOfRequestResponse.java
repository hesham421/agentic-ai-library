package io.agenticai.integration.dto;

import io.agenticai.rpt.contract.CheckSummary;
import io.agenticai.rpt.contract.ChecksOfRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * The API document's {@code ChecksOfRequestResponse} schema (API-INT-006), field for field — the
 * Report Store's list answer, unchanged: at most 100 Checks, newest first, with the total.
 *
 * @param total  every Check of the service code and request number
 * @param checks the newest Checks (at most 100), unmodifiable
 */
public record ChecksOfRequestResponse(long total, List<CheckItem> checks) {

    public ChecksOfRequestResponse {
        checks = List.copyOf(Objects.requireNonNull(checks, "checks"));
    }

    /**
     * One Check of the list.
     *
     * @param checkId          DBF-INT-001 — integer int64
     * @param status           DBF-INT-002 — CHECK_STATUS code
     * @param overallStatus    OVERALL_STATUS code, nullable
     * @param startedAt        ISO-8601 date-time
     * @param endedAt          ISO-8601 date-time, nullable
     * @param employeeDecision DBF-INT-007 — EMPLOYEE_DECISION code, nullable
     */
    public record CheckItem(Long checkId,
                            String status,
                            String overallStatus,
                            OffsetDateTime startedAt,
                            OffsetDateTime endedAt,
                            String employeeDecision) {

        static CheckItem of(CheckSummary summary) {
            return new CheckItem(summary.checkId(), summary.status(), summary.overallStatus(),
                    summary.startedAt(), summary.endedAt(), summary.employeeDecision());
        }
    }

    /** The one mapping of the Report Store's list to the response; no decision is taken here. */
    public static ChecksOfRequestResponse of(ChecksOfRequest list) {
        Objects.requireNonNull(list, "list");
        return new ChecksOfRequestResponse(list.total(),
                list.checks().stream().map(CheckItem::of).toList());
    }
}
