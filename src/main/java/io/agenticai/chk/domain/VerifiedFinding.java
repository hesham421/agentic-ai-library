package io.agenticai.chk.domain;

import java.util.Objects;

/**
 * One finding as the deterministic checks decided it (REQ-CHK-019 … REQ-CHK-029, REQ-CHK-038,
 * REQ-CHK-040): the condition, the outcome code decides, its evidence and the employee's note.
 * A plain value; it holds no document content beyond the evidence text the finding rests on.
 *
 * @param condition the condition, or the required document type
 * @param outcome   the decided outcome
 * @param evidence  the evidence text; {@code null} when none was given
 * @param note      the note for the employee; {@code null} when none
 */
public record VerifiedFinding(String condition, FindingOutcome outcome, String evidence, String note) {

    public VerifiedFinding {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(outcome, "outcome");
    }
}
