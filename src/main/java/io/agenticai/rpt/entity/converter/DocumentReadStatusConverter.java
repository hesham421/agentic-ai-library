package io.agenticai.rpt.entity.converter;

import io.agenticai.rpt.domain.DocumentReadStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link DocumentReadStatus} to {@code RPT_CHECK_DOCUMENT.READ_STATUS} (DBF-RPT-034) through the enum's own
 * stored values ({@link DocumentReadStatus#storedValue()}), exactly the set of {@code CHK_RPT_CHECK_DOCUMENT_READ_STATUS}
 * (RULE-RPT-006, ADR-RPT-011). One mechanism for every closed list RPT stores: each is mapped
 * through its own converter, never with {@code @Enumerated}, so the written value is always the
 * one the enum declares — even where it is not the constant name ({@code FetchMode}).
 *
 * <p>Auto-applied: every {@code DocumentReadStatus} attribute of every RPT entity is stored this way; the
 * entities also name it explicitly with {@code @Convert}.
 */
@Converter(autoApply = true)
public class DocumentReadStatusConverter implements AttributeConverter<DocumentReadStatus, String> {

    @Override
    public String convertToDatabaseColumn(DocumentReadStatus attribute) {
        return attribute == null ? null : attribute.storedValue();
    }

    /**
     * @throws IllegalStateException when the stored value is outside the closed set — the CHECK
     *         constraint makes that impossible, so it is an integrity failure, never a refusal
     *         nor an HTTP error
     */
    @Override
    public DocumentReadStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DocumentReadStatus.fromStored(dbData)
                .orElseThrow(() -> new IllegalStateException(
                        "RPT_CHECK_DOCUMENT.READ_STATUS holds a value outside CHK_RPT_CHECK_DOCUMENT_READ_STATUS: " + dbData));
    }
}
