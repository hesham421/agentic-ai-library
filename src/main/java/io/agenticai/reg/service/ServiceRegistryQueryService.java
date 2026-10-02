package io.agenticai.reg.service;

import io.agenticai.reg.contract.ServiceNotAvailableException;
import io.agenticai.reg.domain.ServiceCodes;
import io.agenticai.reg.dto.LoadResultResponse;
import io.agenticai.reg.dto.ServiceSummary;
import io.agenticai.reg.entity.RequiredDocument;
import io.agenticai.reg.entity.ServicePackage;
import io.agenticai.reg.entity.ServicePackageVersion;
import io.agenticai.reg.error.ServiceRegistryErrorCodes;
import io.agenticai.reg.error.ServiceRegistryException;
import io.agenticai.reg.repository.LoadResultRepository;
import io.agenticai.reg.repository.RequiredDocumentRepository;
import io.agenticai.reg.repository.ServicePackageRepository;
import io.agenticai.reg.repository.ServicePackageVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The reads behind REG's HTTP surface — API-REG-001, API-REG-002, API-REG-003 (ADR-REG-007:
 * read-only; nothing here writes). Answers DTOs that carry exactly the API document's fields:
 * never SQL text, connection settings or request data (REQ-REG-030, REQ-REG-059).
 */
@Service
@Transactional(readOnly = true)
public class ServiceRegistryQueryService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryQueryService.class);

    private final ServicePackageRepository packages;
    private final ServicePackageVersionRepository versions;
    private final RequiredDocumentRepository requiredDocuments;
    private final LoadResultRepository loadResults;

    public ServiceRegistryQueryService(ServicePackageRepository packages,
                                       ServicePackageVersionRepository versions,
                                       RequiredDocumentRepository requiredDocuments,
                                       LoadResultRepository loadResults) {
        this.packages = Objects.requireNonNull(packages, "packages");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.requiredDocuments = Objects.requireNonNull(requiredDocuments, "requiredDocuments");
        this.loadResults = Objects.requireNonNull(loadResults, "loadResults");
    }

    /**
     * API-REG-001 (CON-REG-008) — the available services (QR-REG-001), each with its current
     * version (QR-REG-002) and required document types (QR-REG-003); every row is available.
     */
    public List<ServiceSummary> listServices() {
        log.debug("REG list services");
        List<ServicePackage> available = packages.findByAvailableTrueOrderByServiceCodeAsc();
        List<ServiceSummary> summaries = new ArrayList<>(available.size());
        for (ServicePackage pkg : available) {
            summaries.add(summaryOf(pkg));
        }
        return List.copyOf(summaries);
    }

    /**
     * API-REG-002 (CON-REG-010) — the current version summary of one service, available or
     * withdrawn (ADR-REG-016); the code is matched trimmed and case-insensitively (REQ-REG-064).
     *
     * @throws ServiceNotAvailableException {@code REG-404-SERVICE-NOT-FOUND} when the registry
     *                                      does not hold the code (RULE-REG-016)
     */
    public ServiceSummary getService(String serviceCode) {
        String code = ServiceCodes.canonical(Objects.requireNonNull(serviceCode, "serviceCode"));
        log.debug("REG read service \"{}\"", code);
        ServicePackage pkg = packages.findByServiceCode(code)
                .orElseThrow(() -> new ServiceNotAvailableException(code));
        return summaryOf(pkg);
    }

    /** API-REG-003 — the load report: every Load Result of the latest run (QR-REG-005). */
    public List<LoadResultResponse> readLoadReport() {
        log.debug("REG read load report");
        return loadResults.findLatestRun().stream().map(LoadResultResponse::of).toList();
    }

    private ServiceSummary summaryOf(ServicePackage pkg) {
        // QR-REG-002: no row → the package has no stored version — cannot occur for a registered code → REG-500
        ServicePackageVersion current = versions.findFirstByServicePackageIdOrderByVersionNumberDesc(pkg.getServicePackageId())
                .orElseThrow(() -> new ServiceRegistryException(ServiceRegistryErrorCodes.UNEXPECTED_FAILURE));
        List<String> documentTypes = requiredDocuments
                .findByServicePackageVersionIdOrderByDocumentTypeAsc(current.getServicePackageVersionId())
                .stream()
                .map(RequiredDocument::getDocumentType)
                .toList();
        return ServiceSummary.of(pkg, current, documentTypes);
    }
}
