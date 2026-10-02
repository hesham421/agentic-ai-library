package io.agenticai.doc.contract;

import io.agenticai.doc.error.DocumentAccessException;
import io.agenticai.doc.error.DocumentAccessTexts;

import java.util.Objects;

/**
 * RULE-DOC-001 — the fetch mode of the upload's service package version is not {@code manual},
 * so the upload is rejected (REQ-DOC-020; CON-DOC-003). In-process code
 * {@value DocumentRejectionCodes#FETCH_MODE_NOT_MANUAL} (ADR-DOC-012).
 */
public class FetchModeNotManualException extends DocumentAccessException {

    private final String serviceCode;
    private final String fetchMode;

    /**
     * @param serviceCode the service code of the Check
     * @param fetchMode   the version's fetch mode as its stored value ({@code path} or {@code blob})
     */
    public FetchModeNotManualException(String serviceCode, String fetchMode) {
        super(DocumentRejectionCodes.FETCH_MODE_NOT_MANUAL,
                DocumentAccessTexts.english(DocumentRejectionCodes.FETCH_MODE_NOT_MANUAL,
                        Objects.requireNonNull(serviceCode, "serviceCode"),
                        Objects.requireNonNull(fetchMode, "fetchMode")),
                null);
        this.serviceCode = serviceCode;
        this.fetchMode = fetchMode;
    }

    public String serviceCode() {
        return serviceCode;
    }

    /** The version's fetch mode, as its stored value (CON-DOC-002). */
    public String fetchMode() {
        return fetchMode;
    }
}
