package io.agenticai.reg.entity.converter;

import io.agenticai.reg.domain.LoadOutcome;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link LoadOutcome} to {@code REG_LOAD_RESULT.OUTCOME} (DBF-REG-046) through the enum's
 * own stored values — {@code REGISTERED | UNCHANGED | REJECTED | WITHDRAWN | ACTIVATED | UPDATED |
 * REMOVED}, exactly the set of {@code CHK_REG_LOAD_RESULT_OUTCOME} (REQ-REG-007, ADR-REG-010).
 * The stored value is owned by the enum, so {@code @Enumerated(EnumType.STRING)} is deliberately
 * not used — the constant names are never written.
 *
 * <p>Auto-applied: every {@code LoadOutcome} attribute of every REG entity is stored this way.
 */
@Converter(autoApply = true)
public class LoadOutcomeConverter implements AttributeConverter<LoadOutcome, String> {

    @Override
    public String convertToDatabaseColumn(LoadOutcome attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a load reason
     *         nor an HTTP error
     */
    @Override
    public LoadOutcome convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return LoadOutcome.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "REG_LOAD_RESULT.OUTCOME holds a value outside CHK_REG_LOAD_RESULT_OUTCOME: "
                                + dbData));
    }
}
