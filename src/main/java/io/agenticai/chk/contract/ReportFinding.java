package io.agenticai.chk.contract;

import java.util.Objects;

/**
 * CON-CHK-008 — one finding of a completed Check: a condition of the service or a required
 * document type, its outcome, its evidence and a note for the employee (CON-CHK-002).
 *
 * @param condition the condition text
 * @param outcome   the FINDING_OUTCOME code: {@code SATISFIED}, {@code NOT_SATISFIED} or
 *                  {@code UNDETERMINED}
 * @param evidence  the evidence text; {@code null} when the model gave none (the finding is then
 *                  UNDETERMINED — REQ-CHK-040)
 * @param note      the note for the employee; {@code null} when there is none
 */
public record ReportFinding(String condition, String outcome, String evidence, String note) {

    public ReportFinding {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(outcome, "outcome");
    }
}
