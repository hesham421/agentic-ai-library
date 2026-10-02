package io.agenticai.integration.dto;

import io.agenticai.rpt.contract.RecordedDecision;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The API document's {@code RecordedDecisionResponse} schema (API-INT-004, 201), field for field —
 * the Report Store's recorded decision, unchanged (REQ-INT-022).
 *
 * @param checkId             the Check identifier, integer int64
 * @param employeeDecision    DBF-INT-007 — {@code APPROVED} | {@code REJECTED}
 * @param decidedBy           DBF-INT-008 — the deciding employee, exactly as sent
 * @param decidedAt           the Report Store's recording time, ISO-8601 date-time
 * @param approvalApiExecuted whether the decision was executed through the host Approval API
 */
public record RecordedDecisionResponse(Long checkId,
                                       String employeeDecision,
                                       String decidedBy,
                                       OffsetDateTime decidedAt,
                                       boolean approvalApiExecuted) {

    /** The one mapping of the Report Store's answer to the response; no decision is taken here. */
    public static RecordedDecisionResponse of(RecordedDecision recorded) {
        Objects.requireNonNull(recorded, "recorded");
        return new RecordedDecisionResponse(recorded.checkId(), recorded.employeeDecision(),
                recorded.decidedBy(), recorded.decidedAt(), recorded.approvalApiExecuted());
    }
}
