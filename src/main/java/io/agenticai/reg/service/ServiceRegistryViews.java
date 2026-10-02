package io.agenticai.reg.service;

import io.agenticai.reg.contract.ConnectionSettings;
import io.agenticai.reg.contract.DocumentSource;
import io.agenticai.reg.contract.QueryDefinition;
import io.agenticai.reg.contract.ServicePackageVersionView;
import io.agenticai.reg.contract.ServicePackageView;
import io.agenticai.reg.entity.Connection;
import io.agenticai.reg.entity.RequiredDocument;
import io.agenticai.reg.entity.ServicePackageVersion;
import io.agenticai.reg.entity.ServiceQuery;

import java.util.List;

/**
 * The one place that maps REG entities to the contract's value objects. Holds no decision; every
 * list it builds is unmodifiable (REQ-REG-060). The approval API definition is never copied into
 * a package view (REQ-REG-044).
 */
final class ServiceRegistryViews {

    private ServiceRegistryViews() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    static ServicePackageView currentPackage(String serviceCode,
                                             ServicePackageVersion version,
                                             List<ServiceQuery> queries,
                                             List<RequiredDocument> requiredDocuments) {
        return new ServicePackageView(
                serviceCode,
                version.getVersionNumber(),
                version.getServiceKnowledge(),
                version.getInputName(),
                queries(queries),
                version.getFetchMode().storedValue(),
                documentSource(version),
                documentTypes(requiredDocuments));
    }

    static ServicePackageVersionView storedVersion(String serviceCode,
                                                   ServicePackageVersion version,
                                                   List<ServiceQuery> queries,
                                                   List<RequiredDocument> requiredDocuments) {
        return new ServicePackageVersionView(
                serviceCode,
                version.getVersionNumber(),
                version.getServiceKnowledge(),
                version.getServiceDefinition(),
                version.getInputName(),
                queries(queries),
                version.getFetchMode().storedValue(),
                documentSource(version),
                documentTypes(requiredDocuments),
                version.isApprovalEnabled());
    }

    static ConnectionSettings connection(Connection connection) {
        return new ConnectionSettings(
                connection.getConnectionName(),
                connection.getConnectionType().storedValue(),
                connection.getEndpoint(),
                connection.getQueryTool(),
                connection.getDialect(),
                connection.getCredentialReference(),
                connection.isLimitedToViews());
    }

    /** The document source of a {@code path} / {@code blob} version; {@code null} when the version holds none. */
    static DocumentSource documentSource(ServicePackageVersion version) {
        if (version.getDocumentSourceQueryName() == null || version.getDocumentTypeColumn() == null) {
            return null;
        }
        return new DocumentSource(
                version.getDocumentSourceQueryName(),
                version.getDocumentTypeColumn(),
                version.getDocumentPathColumn(),
                version.getDocumentContentColumn());
    }

    private static List<QueryDefinition> queries(List<ServiceQuery> queries) {
        return queries.stream()
                .map(q -> new QueryDefinition(q.getQueryName(), q.getConnectionName(), q.getSqlText()))
                .toList();
    }

    private static List<String> documentTypes(List<RequiredDocument> requiredDocuments) {
        return requiredDocuments.stream().map(RequiredDocument::getDocumentType).toList();
    }
}
