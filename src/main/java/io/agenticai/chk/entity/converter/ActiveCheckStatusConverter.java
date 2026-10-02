package io.agenticai.chk.entity.converter;

import io.agenticai.chk.domain.ActiveCheckStatus;
import io.agenticai.chk.error.CheckEngineTexts;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link ActiveCheckStatus} to {@code CHK_ACTIVE_CHECK.CHECK_STATUS} (DBF-CHK-003) through the
 * enum's own stored values — {@code AWAITING_DOCUMENTS | RUNNING}, exactly the set of
 * {@code CHK_CHK_ACTIVE_CHECK_CHECK_STATUS} (RULE-CHK-009, ADR-CHK-016). Reading through
 * {@link ActiveCheckStatus#fromStored} rather than {@code @Enumerated} makes a value outside the
 * closed set an explicit integrity failure instead of a generic mapping error.
 *
 * <p>Auto-applied: every {@code ActiveCheckStatus} attribute of every CHK entity is stored this way.
 */
@Converter(autoApply = true)
public class ActiveCheckStatusConverter implements AttributeConverter<ActiveCheckStatus, String> {

    /** RULE-CHK-009's message key in {@code messages.properties} (internal log only). */
    private static final String RULE_009 = "CHK-RULE-009";

    @Override
    public String convertToDatabaseColumn(ActiveCheckStatus attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure carrying RULE-CHK-009's
     *         message (internal log only), never a load reason nor an HTTP error
     */
    @Override
    public ActiveCheckStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ActiveCheckStatus.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(CheckEngineTexts.english(RULE_009, dbData)));
    }
}
