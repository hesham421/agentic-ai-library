package io.agenticai.chk.port;

import io.agenticai.chk.domain.FindingOutcome;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The comparison model's answer, parsed into the report structure that is fixed for every
 * service (REQ-CHK-037): one finding per condition of the service knowledge. Its JSON schema is
 * the output schema sent with every call; an answer that does not parse into it is
 * {@link ModelOutputInvalidException} (REQ-CHK-039). It is data only: the findings are verified
 * in code afterwards (ADR-CHK-003) and nothing in them is ever executed (REQ-CHK-031).
 *
 * <p>The JSON property names are the record component names. {@code outcome} is the
 * {@link FindingOutcome} constant name ({@code SATISFIED}, {@code NOT_SATISFIED},
 * {@code UNDETERMINED}) — the schema lists exactly those three values and any other value does
 * not parse. {@code condition} and {@code outcome} are required; a component marked
 * {@link Nullable} is optional in the schema and may be absent or {@code null} (a finding without
 * evidence is made UNDETERMINED in code — REQ-CHK-040).
 *
 * @param findings the findings; unmodifiable
 */
public record ComparisonOutput(List<Finding> findings) {

    public ComparisonOutput {
        findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
    }

    /**
     * One condition of the service knowledge and the model's view of it.
     *
     * @param condition        the condition, as the service knowledge states it
     * @param outcome          the model's outcome — replaced in code when the deterministic checks
     *                         decide otherwise (REQ-CHK-024 … REQ-CHK-029)
     * @param evidence         the exact text of the Check's data the outcome rests on
     * @param evidenceLocation where the evidence was found — the {@code source} of its data block
     * @param explicit         the value, comparison and limit of an explicit value or date
     *                         condition; {@code null} for any other condition (ADR-CHK-003)
     * @param note             a note for the employee
     */
    public record Finding(String condition,
                          FindingOutcome outcome,
                          @Nullable String evidence,
                          @Nullable String evidenceLocation,
                          @Nullable Explicit explicit,
                          @Nullable String note) {

        public Finding {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    /**
     * An explicit value or date condition as the model read it (ADR-CHK-003): code verifies the
     * value against the Check's data and the limit against the service knowledge, then recomputes
     * the comparison.
     *
     * @param valueFound the value found in the Check's data
     * @param comparison the comparison: {@code >=}, {@code >}, {@code <=}, {@code <}, {@code =},
     *                   {@code before}, {@code after}, {@code on or before} or {@code on or after}
     * @param limit      the limit, as the service knowledge states it
     */
    public record Explicit(String valueFound, String comparison, String limit) {
    }
}
