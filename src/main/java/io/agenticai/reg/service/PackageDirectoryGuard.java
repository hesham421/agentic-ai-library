package io.agenticai.reg.service;

import io.agenticai.reg.port.DirectoryStatus;
import io.agenticai.reg.repository.ServicePackageRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Load run step 3 guard — RULE-REG-023 (REQ-REG-066, ADR-REG-018): the package directory is
 * unavailable when it is {@link DirectoryStatus#MISSING} or {@link DirectoryStatus#UNREADABLE},
 * or {@link DirectoryStatus#EMPTY} while the registry holds at least one available Service
 * Package. Then no package is loaded and no service is withdrawn or restored; the load run
 * records one {@code PACKAGE_DIRECTORY} row instead. An empty directory with no available service
 * is a normal, empty load.
 */
@Component
class PackageDirectoryGuard {

    private final ServicePackageRepository packages;

    PackageDirectoryGuard(ServicePackageRepository packages) {
        this.packages = Objects.requireNonNull(packages, "packages");
    }

    boolean unavailable(DirectoryStatus status) {
        Objects.requireNonNull(status, "status");
        return switch (status) {
            case MISSING, UNREADABLE -> true;
            case EMPTY -> packages.existsByAvailableTrue();
            case READY -> false;
        };
    }
}
