package io.agenticai.reg.repository;

import io.agenticai.reg.entity.RequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence of ENT-REG-004 Required Document ({@code REG_REQ_DOC}). Module-internal. Rows are
 * inserted with their version by the load run and never changed (ADR-REG-003).
 */
public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {

    /**
     * QR-REG-003 — the required document types of one version:
     * {@code WHERE SERVICE_PACKAGE_VERSION_ID = ? ORDER BY DOCUMENT_TYPE} (API-REG-001, API-REG-002,
     * and the in-process supply). Empty for a version that requires none.
     */
    List<RequiredDocument> findByServicePackageVersionIdOrderByDocumentTypeAsc(Long servicePackageVersionId);
}
