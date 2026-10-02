package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessException;
import io.agenticai.doc.error.DocumentAccessTexts;

import java.util.Objects;

/**
 * REQ-DOC-003 — the service package version a handover or a fetch names cannot be resolved
 * through the registry, so nothing is stored and no document is fetched (CON-DOC-003,
 * CON-DOC-004). In-process code {@value DocumentRejectionCodes#SERVICE_VERSION_NOT_FOUND}
 * (ADR-DOC-012). Raised by the registry adapter behind DOC's version lookup port, translating
 * the registry's own not-found refusal.
 */
public class ServiceVersionNotFoundException extends DocumentAccessException {

    private final String serviceCode;
    private final int versionNumber;

    /**
     * @param serviceCode   the service code that was requested
     * @param versionNumber the version number that was requested
     * @param cause         the registry's refusal, or {@code null}
     */
    public ServiceVersionNotFoundException(String serviceCode, int versionNumber, Throwable cause) {
        super(DocumentRejectionCodes.SERVICE_VERSION_NOT_FOUND,
                DocumentAccessTexts.english(DocumentRejectionCodes.SERVICE_VERSION_NOT_FOUND), cause);
        this.serviceCode = Objects.requireNonNull(serviceCode, "serviceCode");
        this.versionNumber = versionNumber;
    }

    public ServiceVersionNotFoundException(String serviceCode, int versionNumber) {
        this(serviceCode, versionNumber, null);
    }

    /** The service code that was requested. */
    public String serviceCode() {
        return serviceCode;
    }

    /** The version number that was requested. */
    public int versionNumber() {
        return versionNumber;
    }
}
