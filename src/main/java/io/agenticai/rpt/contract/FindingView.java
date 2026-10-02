package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * One finding of a completed report (CON-RPT-003; API-RPT-001 schema {@code FindingView}). Every
 * text is returned exactly as stored, as data (REQ-RPT-027).
 *
 * @param position  DBF-RPT-022 — order in the report, from 1
 * @param condition DBF-RPT-023 — the condition text
 * @param outcome   DBF-RPT-024 — FINDING_OUTCOME code: SATISFIED, NOT_SATISFIED or UNDETERMINED
 * @param evidence  DBF-RPT-025 — the actual value found
 * @param note      DBF-RPT-026 — the note for the employee
 */
public record FindingView(int position, String condition, String outcome, String evidence, String note) {

    public FindingView {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(note, "note");
    }
}
