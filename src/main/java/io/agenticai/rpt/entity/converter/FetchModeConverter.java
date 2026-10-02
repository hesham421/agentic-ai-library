package io.agenticai.rpt.entity.converter;

import io.agenticai.rpt.domain.FetchMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link FetchMode} to {@code RPT_CHECK_RUN.FETCH_MODE} (DBF-RPT-004) and {@code RPT_CHECK_DOCUMENT.SOURCE_MODE} (DBF-RPT-033) — {@code path | blob | manual}, through the enum's own
 * stored values ({@link FetchMode#storedValue()}), exactly the set of {@code CHK_RPT_CHECK_RUN_FETCH_MODE} and {@code CHK_RPT_CHECK_DOCUMENT_SOURCE_MODE}
 * (RULE-RPT-006, ADR-RPT-011). One mechanism for every closed list RPT stores: each is mapped
 * through its own converter, never with {@code @Enumerated}, so the written value is always the
 * one the enum declares — even where it is not the constant name ({@code FetchMode}).
 *
 * <p>Auto-applied: every {@code FetchMode} attribute of every RPT entity is stored this way; the
 * entities also name it explicitly with {@code @Convert}.
 */
@Converter(autoApply = true)
public class FetchModeConverter implements AttributeConverter<FetchMode, String> {

    @Override
    public String convertToDatabaseColumn(FetchMode attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a refusal
     *         nor an HTTP error
     */
    @Override
    public FetchMode convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return FetchMode.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "RPT_CHECK_RUN.FETCH_MODE / RPT_CHECK_DOCUMENT.SOURCE_MODE holds a value outside the FETCH_MODE closed list (path | blob | manual): " + dbData));
    }
}
