package io.agenticai.integration.dto;

import io.agenticai.chk.contract.StartedCheck;

import java.util.Objects;

/**
 * The API document's {@code StartedCheckResponse} schema (API-INT-001, 202), field for field. A
 * plain JSON object — no envelope.
 *
 * @param checkId  DBF-INT-001 — the new Check's identifier, integer int64
 * @param status   DBF-INT-002 — CHECK_STATUS code, {@code RUNNING} or {@code AWAITING_DOCUMENTS}
 * @param checkUrl the address of the Check's read, {@code /api/v1/checks/{checkId}} (REQ-INT-005);
 *                 also sent as the {@code Location} header
 */
public record StartedCheckResponse(Long checkId, String status, String checkUrl) {

    /** The Report Store's read of a Check (API-RPT-001), the address the host polls (REQ-INT-005). */
    static final String CHECK_READ_PATH = "/api/v1/checks/";

    /** The one mapping of the Check Engine's answer to the response; no decision is taken here. */
    public static StartedCheckResponse of(StartedCheck started) {
        Objects.requireNonNull(started, "started");
        return new StartedCheckResponse(started.checkId(), started.status(),
                CHECK_READ_PATH + started.checkId());
    }
}
