package io.agenticai.reg.service;

import io.agenticai.reg.domain.LoadOutcome;
import io.agenticai.reg.domain.LoadSubject;
import io.agenticai.reg.entity.ServicePackage;
import io.agenticai.reg.entity.ServicePackageVersion;
import io.agenticai.reg.repository.ServicePackageRepository;
import io.agenticai.reg.repository.ServicePackageVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Load run step 5 — withdraw / restore (REQ-REG-010, REQ-REG-011, ADR-REG-007): a stored service
 * code that no folder declares is withdrawn ({@code available = 0}, {@code withdrawnAt = now},
 * one {@code WITHDRAWN} row); a withdrawn code whose folder was accepted this run
 * ({@code REGISTERED} or {@code UNCHANGED}) is made available again. A code whose folder is
 * present but rejected keeps its previous state. Stored versions always stay (REQ-REG-026).
 * Each package is processed behind its own savepoint.
 */
@Component
class ServiceWithdrawal {

    private static final Logger log = LoggerFactory.getLogger(ServiceWithdrawal.class);

    private final ServicePackageRepository packages;
    private final ServicePackageVersionRepository versions;
    private final LoadResultRecorder recorder;

    ServiceWithdrawal(ServicePackageRepository packages,
                      ServicePackageVersionRepository versions,
                      LoadResultRecorder recorder) {
        this.packages = Objects.requireNonNull(packages, "packages");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.recorder = Objects.requireNonNull(recorder, "recorder");
    }

    /**
     * @param declaredCodes the canonical codes every folder of this run declared, valid or not
     * @param acceptedCodes the canonical codes whose folder was accepted this run
     */
    void apply(OffsetDateTime loadRunAt,
               Set<String> declaredCodes,
               Set<String> acceptedCodes,
               ItemSavepoints savepoints,
               LoadRunTally tally) {
        for (ServicePackage stored : packages.findAll()) {
            String code = stored.getServiceCode();
            Long packageId = stored.getServicePackageId();
            if (!declaredCodes.contains(code) && stored.isAvailable()) {
                savepoints.run(code, () -> {
                    ServicePackage pkg = packages.findById(packageId).orElseThrow();
                    pkg.withdraw(loadRunAt);
                    packages.save(pkg);
                    Integer currentVersion = versions.findFirstByServicePackageIdOrderByVersionNumberDesc(packageId)
                            .map(ServicePackageVersion::getVersionNumber)
                            .orElse(null);
                    recorder.record(loadRunAt, LoadSubject.SERVICE_PACKAGE, code, code, currentVersion,
                            LoadOutcome.WITHDRAWN, null);
                    tally.withdrawn++;
                    log.info("REG withdrew service \"{}\": no package folder declares it", code);
                });
            } else if (acceptedCodes.contains(code) && !stored.isAvailable()) {
                savepoints.run(code, () -> {
                    ServicePackage pkg = packages.findById(packageId).orElseThrow();
                    pkg.makeAvailable();
                    packages.save(pkg);
                    tally.restored++;
                    log.info("REG restored service \"{}\": its package folder is back", code);
                });
            }
        }
    }
}
