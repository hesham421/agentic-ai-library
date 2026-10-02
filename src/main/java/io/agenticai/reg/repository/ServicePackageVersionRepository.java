package io.agenticai.reg.repository;

import io.agenticai.reg.domain.FetchMode;
import io.agenticai.reg.entity.ServicePackageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence of ENT-REG-002 Service Package Version ({@code REG_SVC_PKG_VER}). Module-internal.
 *
 * <p>Versions are inserted by the load run and never updated or deleted (ADR-REG-003) — only
 * {@code save} of a new instance is ever used for writing. Every read is QR-REG-002 or one the
 * plan's load run / in-process interface prescribes inline (ADR-REG-011).
 */
public interface ServicePackageVersionRepository extends JpaRepository<ServicePackageVersion, Long> {

    /**
     * QR-REG-002 — the current version of a package, its highest stored version number:
     * {@code ORDER BY VERSION_NUMBER DESC FETCH FIRST 1 ROW ONLY} (API-REG-001, API-REG-002,
     * CON-REG-007, the load run's step 4).
     */
    Optional<ServicePackageVersion> findFirstByServicePackageIdOrderByVersionNumberDesc(Long servicePackageId);

    /**
     * One stored version by its business key (CON-REG-009, CON-REG-012; RULE-REG-003 / RULE-REG-004
     * in the load run's step 4).
     */
    Optional<ServicePackageVersion> findByServicePackageIdAndVersionNumber(Long servicePackageId, Integer versionNumber);

    /**
     * RULE-REG-025 (ADR-REG-021) — every stored version with fetch mode {@code blob} whose document
     * source query ({@code DOCUMENT_SOURCE_QUERY_NAME}) is a query of that version
     * ({@code REG_SVC_QUERY} joined on version id and query name) running on {@code connectionName}.
     * A fixed query of this unit with bound parameters only; the caller passes
     * {@link FetchMode#BLOB}. Ordered by service code and version number so the first row is a
     * stable subject for the load reason message.
     */
    @Query("""
            select new io.agenticai.reg.repository.BlobVersionReference(p.serviceCode, v.versionNumber)
            from ServicePackageVersion v, ServiceQuery q, ServicePackage p
            where v.fetchMode = :fetchMode
              and p.servicePackageId = v.servicePackageId
              and q.servicePackageVersionId = v.servicePackageVersionId
              and q.queryName = v.documentSourceQueryName
              and q.connectionName = :connectionName
            order by p.serviceCode, v.versionNumber
            """)
    List<BlobVersionReference> findBlobVersionsReadingThrough(@Param("connectionName") String connectionName,
                                                              @Param("fetchMode") FetchMode fetchMode);
}
