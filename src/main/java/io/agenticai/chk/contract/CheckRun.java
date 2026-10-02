package io.agenticai.chk.contract;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * CON-CHK-010 — one Check as the result port answers it.
 *
 * @param checkId       the Check identifier
 * @param status        the CHECK_STATUS code (CON-CHK-001)
 * @param serviceCode   the service code
 * @param versionNumber the service package version the Check pinned (RULE-CHK-007)
 * @param fetchMode     the fetch mode code
 * @param requestNumber the request number exactly as received
 * @param employeeId    the employee identity exactly as received
 * @param startedAt     the start time
 */
public record CheckRun(Long checkId,
                       String status,
                       String serviceCode,
                       int versionNumber,
                       String fetchMode,
                       String requestNumber,
                       String employeeId,
                       OffsetDateTime startedAt) {

    public CheckRun {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(fetchMode, "fetchMode");
        Objects.requireNonNull(requestNumber, "requestNumber");
        Objects.requireNonNull(employeeId, "employeeId");
        Objects.requireNonNull(startedAt, "startedAt");
    }
}
