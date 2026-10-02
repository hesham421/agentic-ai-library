package io.agenticai.integration.dto;

import io.agenticai.chk.contract.ConfirmedCheck;

import java.util.Objects;

/**
 * The API document's {@code ConfirmedCheckResponse} schema (API-INT-003, 202), field for field.
 *
 * @param checkId DBF-INT-001 — the Check identifier, integer int64
 * @param status  DBF-INT-002 — CHECK_STATUS code, {@code RUNNING}
 */
public record ConfirmedCheckResponse(Long checkId, String status) {

    /** The one mapping of the Check Engine's answer to the response; no decision is taken here. */
    public static ConfirmedCheckResponse of(ConfirmedCheck confirmed) {
        Objects.requireNonNull(confirmed, "confirmed");
        return new ConfirmedCheckResponse(confirmed.checkId(), confirmed.status());
    }
}
