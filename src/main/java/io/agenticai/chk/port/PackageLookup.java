package io.agenticai.chk.port;

import io.agenticai.chk.contract.ConnectionNotActivatedException;
import io.agenticai.chk.contract.ServiceNotAvailableException;

import java.util.Optional;

/**
 * The service package version of a Check (REQ-CHK-007, REQ-CHK-008; RULE-CHK-007): the CHK port
 * over the service registry's package reads. Implemented by an integration adapter over REG's
 * published in-process interface, never in this module's own code. The approval API definition is
 * never read through it (REQ-CHK-032).
 */
public interface PackageLookup {

    /**
     * The version current at the moment of the call — read once, at a Check's start.
     *
     * @param serviceCode the service code exactly as received
     * @return the version, whole
     * @throws ServiceNotAvailableException    the registry reports the service not available
     * @throws ConnectionNotActivatedException a query of the version names a connection that is not
     *                                         activated in this environment (REQ-CHK-006)
     */
    ServicePackageSnapshot currentPackage(String serviceCode);

    /**
     * Exactly the version recorded at the Check's start — read when a {@code manual} Check resumes
     * (RULE-CHK-007), whatever version is current now.
     *
     * @return the version; empty when it cannot be resolved (the Check then ends FAILED /
     *         INTERNAL_ERROR with RULE-CHK-007's message)
     */
    Optional<ServicePackageSnapshot> pinnedPackage(String serviceCode, int versionNumber);
}
