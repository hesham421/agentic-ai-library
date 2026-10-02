package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessException;
import io.agenticai.doc.error.DocumentAccessTexts;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * RULE-DOC-002 — the document type given with the upload is not one of the required document
 * types of its service package version, so the upload is rejected (REQ-DOC-021; CON-DOC-003).
 * In-process code {@value DocumentRejectionCodes#DOCUMENT_TYPE_NOT_OF_SERVICE} (ADR-DOC-012).
 * The message lists the version's required types, comma-separated in their declared order.
 */
public class DocumentTypeNotOfServiceException extends DocumentAccessException {

    private static final String SEPARATOR = ", ";

    private final String documentType;
    private final String serviceCode;
    private final List<String> requiredDocumentTypes;

    /**
     * @param documentType          the document type the employee gave
     * @param serviceCode           the service code of the Check
     * @param requiredDocumentTypes the version's required document types, in declared order
     */
    public DocumentTypeNotOfServiceException(String documentType,
                                             String serviceCode,
                                             Collection<String> requiredDocumentTypes) {
        super(DocumentRejectionCodes.DOCUMENT_TYPE_NOT_OF_SERVICE,
                DocumentAccessTexts.english(DocumentRejectionCodes.DOCUMENT_TYPE_NOT_OF_SERVICE,
                        Objects.requireNonNull(documentType, "documentType"),
                        Objects.requireNonNull(serviceCode, "serviceCode"),
                        String.join(SEPARATOR, Objects.requireNonNull(requiredDocumentTypes, "requiredDocumentTypes"))),
                null);
        this.documentType = documentType;
        this.serviceCode = serviceCode;
        this.requiredDocumentTypes = List.copyOf(requiredDocumentTypes);
    }

    public String documentType() {
        return documentType;
    }

    public String serviceCode() {
        return serviceCode;
    }

    /** The version's required document types, unmodifiable, in declared order. */
    public List<String> requiredDocumentTypes() {
        return requiredDocumentTypes;
    }
}
