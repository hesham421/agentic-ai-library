package io.agenticai.reg.entity.converter;

import io.agenticai.reg.domain.FetchMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link FetchMode} to {@code REG_SVC_PKG_VER.FETCH_MODE} (DBF-REG-011) through the enum's
 * own stored values — {@code path | blob | manual}, exactly the set of
 * {@code CHK_REG_SVC_PKG_VER_FETCH_MODE} (RULE-REG-008, ADR-REG-010). The constant names are
 * never written, so {@code @Enumerated(EnumType.STRING)} is deliberately not used.
 *
 * <p>Auto-applied: every {@code FetchMode} attribute of every REG entity is stored this way.
 */
@Converter(autoApply = true)
public class FetchModeConverter implements AttributeConverter<FetchMode, String> {

    @Override
    public String convertToDatabaseColumn(FetchMode attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a load reason
     *         nor an HTTP error
     */
    @Override
    public FetchMode convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return FetchMode.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "REG_SVC_PKG_VER.FETCH_MODE holds a value outside CHK_REG_SVC_PKG_VER_FETCH_MODE: "
                                + dbData));
    }
}
