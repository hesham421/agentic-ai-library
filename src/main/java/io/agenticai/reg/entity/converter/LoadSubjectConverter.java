package io.agenticai.reg.entity.converter;

import io.agenticai.reg.domain.LoadSubject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link LoadSubject} to {@code REG_LOAD_RESULT.SUBJECT_KIND} (DBF-REG-042) through the
 * enum's own stored values — {@code SERVICE_PACKAGE | CONNECTION | PACKAGE_DIRECTORY}, exactly
 * the set of {@code CHK_REG_LOAD_RESULT_SUBJECT_KIND} (REQ-REG-066, ADR-REG-010, ADR-REG-018).
 * The stored value is owned by the enum, so {@code @Enumerated(EnumType.STRING)} is deliberately
 * not used — the constant names are never written.
 *
 * <p>Auto-applied: every {@code LoadSubject} attribute of every REG entity is stored this way.
 */
@Converter(autoApply = true)
public class LoadSubjectConverter implements AttributeConverter<LoadSubject, String> {

    @Override
    public String convertToDatabaseColumn(LoadSubject attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a load reason
     *         nor an HTTP error
     */
    @Override
    public LoadSubject convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return LoadSubject.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "REG_LOAD_RESULT.SUBJECT_KIND holds a value outside CHK_REG_LOAD_RESULT_SUBJECT_KIND: "
                                + dbData));
    }
}
