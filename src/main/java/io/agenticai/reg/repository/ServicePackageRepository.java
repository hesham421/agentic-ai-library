package io.agenticai.reg.repository;

import io.agenticai.reg.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence of ENT-REG-001 Service Package ({@code REG_SVC_PKG}). Module-internal: injected only
 * by the REG services; other modules read REG through {@code io.agenticai.reg.contract}.
 *
 * <p>Every method is one catalogued read (QR-REG-001, QR-REG-004) or one need of the start-up
 * load run, whose persistence the plan specifies inline and does not catalogue under {@code QR-*}
 * (ADR-REG-011). Reads run under the calling service's read-only transaction.
 */
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    /**
     * QR-REG-001 — the available packages, ordered by service code:
     * {@code WHERE AVAILABLE = 1 ORDER BY SERVICE_CODE} (API-REG-001).
     */
    List<ServicePackage> findByAvailableTrueOrderByServiceCodeAsc();

    /**
     * QR-REG-004 — the package of one canonical service code, available or withdrawn
     * (API-REG-002, the in-process interface, and the load run's step 4).
     *
     * @param serviceCode the canonical (trimmed, lower-case) code — REQ-REG-064
     */
    Optional<ServicePackage> findByServiceCode(String serviceCode);

    /**
     * Load run, step 3 guard (RULE-REG-023, ADR-REG-018): whether {@code REG_SVC_PKG} holds at
     * least one row with {@code AVAILABLE = 1} — an existence check, the row is never loaded.
     */
    boolean existsByAvailableTrue();
}
