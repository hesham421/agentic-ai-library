package io.agenticai.reg.service;

import io.agenticai.reg.contract.ApprovalApi;
import io.agenticai.reg.contract.ApprovalApiRegistry;
import io.agenticai.reg.contract.ConnectionNotFoundException;
import io.agenticai.reg.contract.ConnectionSettings;
import io.agenticai.reg.contract.ServiceConnectionNotActivatedException;
import io.agenticai.reg.contract.ServiceNotAvailableException;
import io.agenticai.reg.contract.ServicePackageVersionView;
import io.agenticai.reg.contract.ServicePackageView;
import io.agenticai.reg.contract.ServiceRegistry;
import io.agenticai.reg.contract.VersionNotFoundException;
import io.agenticai.reg.domain.ServiceCodes;
import io.agenticai.reg.entity.RequiredDocument;
import io.agenticai.reg.entity.ServicePackage;
import io.agenticai.reg.entity.ServicePackageVersion;
import io.agenticai.reg.entity.ServiceQuery;
import io.agenticai.reg.error.ServiceRegistryErrorCodes;
import io.agenticai.reg.error.ServiceRegistryException;
import io.agenticai.reg.repository.ConnectionRepository;
import io.agenticai.reg.repository.RequiredDocumentRepository;
import io.agenticai.reg.repository.ServicePackageRepository;
import io.agenticai.reg.repository.ServicePackageVersionRepository;
import io.agenticai.reg.repository.ServiceQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * The in-process interface of REG — {@link ServiceRegistry} for CHK, DOC and RPT, and
 * {@link ApprovalApiRegistry} for INT's Employee Decision operation (CON-REG-007, CON-REG-009,
 * CON-REG-011 … CON-REG-013; ADR-REG-011). Every method canonicalises the received service code
 * first (REQ-REG-064), reads only, and returns immutable value objects — never an entity
 * (REQ-REG-060). Reads are at debug level; no knowledge, SQL text or credential reference is
 * ever logged.
 */
@Service
@Transactional(readOnly = true)
public class ServiceRegistryService implements ServiceRegistry, ApprovalApiRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryService.class);

    private final ServicePackageRepository packages;
    private final ServicePackageVersionRepository versions;
    private final ServiceQueryRepository queries;
    private final RequiredDocumentRepository requiredDocuments;
    private final ConnectionRepository connections;

    public ServiceRegistryService(ServicePackageRepository packages,
                                  ServicePackageVersionRepository versions,
                                  ServiceQueryRepository queries,
                                  RequiredDocumentRepository requiredDocuments,
                                  ConnectionRepository connections) {
        this.packages = Objects.requireNonNull(packages, "packages");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.queries = Objects.requireNonNull(queries, "queries");
        this.requiredDocuments = Objects.requireNonNull(requiredDocuments, "requiredDocuments");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public ServicePackageView getCurrentServicePackage(String serviceCode) {
        String code = ServiceCodes.canonical(serviceCode);
        log.debug("REG supply current package of \"{}\"", code);
        // RULE-REG-016 — unknown or withdrawn
        ServicePackage pkg = packages.findByServiceCode(code)
                .filter(ServicePackage::isAvailable)
                .orElseThrow(() -> new ServiceNotAvailableException(code));
        ServicePackageVersion current = currentVersion(pkg);
        List<ServiceQuery> queryRows = queries.findByServicePackageVersionIdOrderByQueryNameAsc(current.getServicePackageVersionId());
        // RULE-REG-017 — every query's connection activated
        for (ServiceQuery query : queryRows) {
            if (!connections.existsByConnectionName(query.getConnectionName())) {
                throw new ServiceConnectionNotActivatedException(code, query.getConnectionName());
            }
        }
        List<RequiredDocument> documentRows =
                requiredDocuments.findByServicePackageVersionIdOrderByDocumentTypeAsc(current.getServicePackageVersionId());
        return ServiceRegistryViews.currentPackage(code, current, queryRows, documentRows);
    }

    @Override
    public ServicePackageVersionView getServicePackageVersion(String serviceCode, int versionNumber) {
        String code = ServiceCodes.canonical(serviceCode);
        log.debug("REG resolve version {} of \"{}\"", versionNumber, code);
        ServicePackageVersion version = storedVersion(code, versionNumber);
        List<ServiceQuery> queryRows = queries.findByServicePackageVersionIdOrderByQueryNameAsc(version.getServicePackageVersionId());
        List<RequiredDocument> documentRows =
                requiredDocuments.findByServicePackageVersionIdOrderByDocumentTypeAsc(version.getServicePackageVersionId());
        return ServiceRegistryViews.storedVersion(code, version, queryRows, documentRows);
    }

    @Override
    public ConnectionSettings getConnection(String connectionName) {
        Objects.requireNonNull(connectionName, "connectionName");
        log.debug("REG supply connection \"{}\"", connectionName);
        return connections.findByConnectionName(connectionName)
                .map(ServiceRegistryViews::connection)
                .orElseThrow(() -> new ConnectionNotFoundException(connectionName));
    }

    @Override
    public boolean isServiceAvailable(String serviceCode) {
        String code = ServiceCodes.canonical(serviceCode);
        return packages.findByServiceCode(code).map(ServicePackage::isAvailable).orElse(false);
    }

    @Override
    public ApprovalApi getApprovalApi(String serviceCode, int versionNumber) {
        String code = ServiceCodes.canonical(serviceCode);
        log.debug("REG supply approval API of \"{}\" version {}", code, versionNumber);
        ServicePackageVersion version = storedVersion(code, versionNumber);
        boolean enabled = version.isApprovalEnabled();
        return new ApprovalApi(enabled, enabled ? version.getApprovalApi() : null);
    }

    private ServicePackageVersion currentVersion(ServicePackage pkg) {
        return versions.findFirstByServicePackageIdOrderByVersionNumberDesc(pkg.getServicePackageId())
                .orElseThrow(() -> new ServiceRegistryException(ServiceRegistryErrorCodes.UNEXPECTED_FAILURE));
    }

    private ServicePackageVersion storedVersion(String code, int versionNumber) {
        ServicePackage pkg = packages.findByServiceCode(code)
                .orElseThrow(() -> new VersionNotFoundException(code, versionNumber));
        return versions.findByServicePackageIdAndVersionNumber(pkg.getServicePackageId(), versionNumber)
                .orElseThrow(() -> new VersionNotFoundException(code, versionNumber));
    }
}
