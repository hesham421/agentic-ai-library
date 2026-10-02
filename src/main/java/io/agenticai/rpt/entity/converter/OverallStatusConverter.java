package io.agenticai.rpt.entity.converter;

import io.agenticai.rpt.domain.OverallStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link OverallStatus} to {@code RPT_CHECK_RUN.OVERALL_STATUS} (DBF-RPT-011) through the enum's own
 * stored values ({@link OverallStatus#storedValue()}), exactly the set of {@code CHK_RPT_CHECK_RUN_OVERALL_STATUS}
 * (RULE-RPT-006, ADR-RPT-011). One mechanism for every closed list RPT stores: each is mapped
 * through its own converter, never with {@code @Enumerated}, so the written value is always the
 * one the enum declares — even where it is not the constant name ({@code FetchMode}).
 *
 * <p>Auto-applied: every {@code OverallStatus} attribute of every RPT entity is stored this way; the
 * entities also name it explicitly with {@code @Convert}.
 */
@Converter(autoApply = true)
public class OverallStatusConverter implements AttributeConverter<OverallStatus, String> {

    @Override
    public String convertToDatabaseColumn(OverallStatus attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a refusal
     *         nor an HTTP error
     */
    @Override
    public OverallStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return OverallStatus.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "RPT_CHECK_RUN.OVERALL_STATUS holds a value outside CHK_RPT_CHECK_RUN_OVERALL_STATUS: " + dbData));
    }
}
