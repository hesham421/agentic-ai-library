package io.agenticai.chk.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * CON-CHK-008 — the metadata handed with every report (REQ-CHK-045).
 *
 * @param serviceCode     the service code
 * @param versionNumber   the service package version the Check pinned
 * @param fetchMode       the fetch mode code
 * @param comparisonModel the comparison model identifier ({@code aias.check.comparison-model.model})
 * @param employeeId      the employee identity exactly as received
 * @param startedAt       the start time of the Check
 * @param endedAt         the end time of the Check
 */
public record ReportMetadata(String serviceCode,
                             int versionNumber,
                             String fetchMode,
                             String comparisonModel,
                             String employeeId,
                             OffsetDateTime startedAt,
                             OffsetDateTime endedAt) {

    public ReportMetadata {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(fetchMode, "fetchMode");
        Objects.requireNonNull(employeeId, "employeeId");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
    }
}
