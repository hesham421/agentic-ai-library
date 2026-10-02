package io.agenticai.reg.dto;

import io.agenticai.reg.entity.ServicePackage;
import io.agenticai.reg.entity.ServicePackageVersion;

import java.util.List;
import java.util.Objects;

/**
 * The API document's {@code ServiceSummary} schema (API-REG-001, API-REG-002), field for field.
 * A plain JSON object — no envelope; never SQL text, a connection setting or request data
 * (REQ-REG-030, REQ-REG-059). Documented here in Javadoc: springdoc is not on the classpath.
 *
 * @param serviceCode           canonical lower-case service code (DBF-REG-002, ADR-REG-017);
 *                              {@code ^[a-z0-9]+(-[a-z0-9]+)*$}, at most 100 characters
 * @param available             {@code true} when the Service Package is available, {@code false}
 *                              when withdrawn (DBF-REG-003, ADR-REG-016)
 * @param versionNumber         the current version number, at least 1 (DBF-REG-007)
 * @param fetchMode             {@code path}, {@code blob} or {@code manual} — the stored value of
 *                              the closed enum (DBF-REG-011)
 * @param requiredDocumentTypes the required document types of the current version, by document
 *                              type (DBF-REG-027); empty when none
 * @param approvalEnabled       whether the current version enables the approval API (DBF-REG-016)
 */
public record ServiceSummary(String serviceCode,
                             boolean available,
                             int versionNumber,
                             String fetchMode,
                             List<String> requiredDocumentTypes,
                             boolean approvalEnabled) {

    public ServiceSummary {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(fetchMode, "fetchMode");
        requiredDocumentTypes = requiredDocumentTypes == null ? List.of() : List.copyOf(requiredDocumentTypes);
    }

    /**
     * The one mapping of a package, its current version and the version's document types to the
     * summary (A.3.8). No decision is taken here.
     */
    public static ServiceSummary of(ServicePackage servicePackage,
                                    ServicePackageVersion currentVersion,
                                    List<String> requiredDocumentTypes) {
        Objects.requireNonNull(servicePackage, "servicePackage");
        Objects.requireNonNull(currentVersion, "currentVersion");
        return new ServiceSummary(
                servicePackage.getServiceCode(),
                servicePackage.isAvailable(),
                currentVersion.getVersionNumber(),
                currentVersion.getFetchMode().storedValue(),
                requiredDocumentTypes,
                currentVersion.isApprovalEnabled());
    }
}
