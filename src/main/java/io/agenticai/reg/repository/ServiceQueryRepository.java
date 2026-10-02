package io.agenticai.reg.repository;

import io.agenticai.reg.entity.ServiceQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence of ENT-REG-003 Service Query ({@code REG_SVC_QUERY}). Module-internal. Rows are
 * inserted with their version by the load run and never changed (ADR-REG-003).
 */
public interface ServiceQueryRepository extends JpaRepository<ServiceQuery, Long> {

    /**
     * The queries of one stored version, by query name (CON-REG-007, CON-REG-009 — the in-process
     * supply; prescribed inline by the plan, ADR-REG-011).
     */
    List<ServiceQuery> findByServicePackageVersionIdOrderByQueryNameAsc(Long servicePackageVersionId);
}
