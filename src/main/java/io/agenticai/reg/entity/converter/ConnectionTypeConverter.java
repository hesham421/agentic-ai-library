package io.agenticai.reg.entity.converter;

import io.agenticai.reg.domain.ConnectionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link ConnectionType} to {@code REG_CONNECTION.CONNECTION_TYPE} (DBF-REG-031) through
 * the enum's own stored values — {@code mcp | jdbc}, exactly the set of
 * {@code CHK_REG_CONNECTION_CONNECTION_TYPE} (RULE-REG-014, ADR-REG-010). The constant names are
 * never written, so {@code @Enumerated(EnumType.STRING)} is deliberately not used.
 *
 * <p>Auto-applied: every {@code ConnectionType} attribute of every REG entity is stored this way.
 */
@Converter(autoApply = true)
public class ConnectionTypeConverter implements AttributeConverter<ConnectionType, String> {

    @Override
    public String convertToDatabaseColumn(ConnectionType attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a load reason
     *         nor an HTTP error
     */
    @Override
    public ConnectionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ConnectionType.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "REG_CONNECTION.CONNECTION_TYPE holds a value outside CHK_REG_CONNECTION_CONNECTION_TYPE: "
                                + dbData));
    }
}
