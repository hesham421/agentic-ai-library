package io.agenticai.reg.contract;

import java.util.Objects;

/**
 * CON-REG-009 / CON-REG-012 "not found" — no such service code or version is stored. An in-process
 * refusal; the contract states no code for it, so none is carried (recorded as an
 * {@code api_doc_gaps} row of this unit). It is not an HTTP error and never a load reason.
 */
public class VersionNotFoundException extends RuntimeException {

    private final String serviceCode;
    private final int versionNumber;

    /**
     * @param serviceCode   the canonical service code that was requested
     * @param versionNumber the version number that was requested
     */
    public VersionNotFoundException(String serviceCode, int versionNumber) {
        super("The registry holds no version " + versionNumber + " of the service \""
                + Objects.requireNonNull(serviceCode, "serviceCode") + "\".");
        this.serviceCode = serviceCode;
        this.versionNumber = versionNumber;
    }

    public String serviceCode() {
        return serviceCode;
    }

    public int versionNumber() {
        return versionNumber;
    }
}
