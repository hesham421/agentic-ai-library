package io.agenticai.rpt.entity.converter;

import io.agenticai.rpt.domain.FindingOutcome;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link FindingOutcome} to {@code RPT_FINDING.FINDING_OUTCOME} (DBF-RPT-024) through the enum's own
 * stored values ({@link FindingOutcome#storedValue()}), exactly the set of {@code CHK_RPT_FINDING_FINDING_OUTCOME}
 * (RULE-RPT-006, ADR-RPT-011). One mechanism for every closed list RPT stores: each is mapped
 * through its own converter, never with {@code @Enumerated}, so the written value is always the
 * one the enum declares — even where it is not the constant name ({@code FetchMode}).
 *
 * <p>Auto-applied: every {@code FindingOutcome} attribute of every RPT entity is stored this way; the
 * entities also name it explicitly with {@code @Convert}.
 */
@Converter(autoApply = true)
public class FindingOutcomeConverter implements AttributeConverter<FindingOutcome, String> {

    @Override
    public String convertToDatabaseColumn(FindingOutcome attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a refusal
     *         nor an HTTP error
     */
    @Override
    public FindingOutcome convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return FindingOutcome.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "RPT_FINDING.FINDING_OUTCOME holds a value outside CHK_RPT_FINDING_FINDING_OUTCOME: " + dbData));
    }
}
