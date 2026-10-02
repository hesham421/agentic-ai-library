package io.agenticai.rpt.entity.converter;

import io.agenticai.rpt.domain.CheckFailureReason;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link CheckFailureReason} to {@code RPT_CHECK_RUN.FAILURE_REASON} (DBF-RPT-013) through the enum's own
 * stored values ({@link CheckFailureReason#storedValue()}), exactly the set of {@code CHK_RPT_CHECK_RUN_FAILURE_REASON}
 * (RULE-RPT-006, ADR-RPT-011). One mechanism for every closed list RPT stores: each is mapped
 * through its own converter, never with {@code @Enumerated}, so the written value is always the
 * one the enum declares — even where it is not the constant name ({@code FetchMode}).
 *
 * <p>Auto-applied: every {@code CheckFailureReason} attribute of every RPT entity is stored this way; the
 * entities also name it explicitly with {@code @Convert}.
 */
@Converter(autoApply = true)
public class CheckFailureReasonConverter implements AttributeConverter<CheckFailureReason, String> {

    @Override
    public String convertToDatabaseColumn(CheckFailureReason attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a refusal
     *         nor an HTTP error
     */
    @Override
    public CheckFailureReason convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CheckFailureReason.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "RPT_CHECK_RUN.FAILURE_REASON holds a value outside CHK_RPT_CHECK_RUN_FAILURE_REASON: " + dbData));
    }
}
