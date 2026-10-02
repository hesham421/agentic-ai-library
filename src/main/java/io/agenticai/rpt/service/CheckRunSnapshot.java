package io.agenticai.rpt.service;

import io.agenticai.rpt.domain.CheckStatus;
import io.agenticai.rpt.domain.FetchMode;

import java.time.OffsetDateTime;

/**
 * The values of one Check Run the result port's {@code getCheck} answers (QR-RPT-001 projected,
 * REQ-RPT-021) — never the entity. Module-internal: the adapter turns it into the Check Engine's
 * value type.
 */
public record CheckRunSnapshot(Long checkId,
                               CheckStatus status,
                               String serviceCode,
                               int versionNumber,
                               FetchMode fetchMode,
                               String requestNumber,
                               String employeeId,
                               OffsetDateTime startedAt) {
}
