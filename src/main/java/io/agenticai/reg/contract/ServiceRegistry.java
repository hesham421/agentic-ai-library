package io.agenticai.reg.contract;

/**
 * The in-process interface of the Service Registry (REG) that CHK, DOC and RPT are given
 * (contract-reg.md; profile {@code module_interface: in_process}). This package is the only REG
 * package another module may import.
 *
 * <p>Every method canonicalises a received service code first — trimmed, lower case
 * (REQ-REG-064, ADR-REG-017) — is read-only, and returns immutable value objects (Java records
 * with unmodifiable lists — REQ-REG-060). The service knowledge is a separate field from the
 * queries and the document settings (REQ-REG-018, REQ-REG-030); no method returns request data
 * (REQ-REG-059). The approval API definition is not here: it is supplied through
 * {@link ApprovalApiRegistry} to the Employee Decision path only (REQ-REG-044).
 *
 * <p>Version pinning (ADR-REG-020): {@link #getCurrentServicePackage} is the resolution step of a
 * new Check and a non-Check current read; every read inside a running Check uses
 * {@link #getServicePackageVersion} with the Check's pinned version number.
 */
public interface ServiceRegistry {

    /**
     * CON-REG-007 — the current version of an available service: the version current at the
     * moment of the call (REQ-REG-024, REQ-REG-027, REQ-REG-029).
     *
     * @param serviceCode the service code, matched trimmed and case-insensitively
     * @throws ServiceNotAvailableException          for an unknown or withdrawn code (RULE-REG-016, REQ-REG-012)
     * @throws ServiceConnectionNotActivatedException when a query of the current version names a
     *                                               connection the environment does not register
     *                                               (RULE-REG-017, REQ-REG-053)
     */
    ServicePackageView getCurrentServicePackage(String serviceCode);

    /**
     * CON-REG-009 — the exact stored version, whatever version is current and whether or not the
     * service was withdrawn since (REQ-REG-025, REQ-REG-070, ADR-REG-003).
     *
     * @throws VersionNotFoundException when no such service code or version is stored
     */
    ServicePackageVersionView getServicePackageVersion(String serviceCode, int versionNumber);

    /**
     * CON-REG-011 — the settings of a registered connection (REQ-REG-048). Never the credential:
     * {@link ConnectionSettings#credentialReference()} is a name in the environment's secret store.
     *
     * @throws ConnectionNotFoundException when no connection of that name is activated in this environment
     */
    ConnectionSettings getConnection(String connectionName);

    /**
     * CON-REG-013 — whether new Checks may use the service code (REQ-REG-012): {@code true} only
     * for a code the registry holds whose package is available. An unknown code answers
     * {@code false}; nothing is thrown.
     */
    boolean isServiceAvailable(String serviceCode);
}
