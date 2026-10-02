package io.agenticai.integration.adapter;

import io.agenticai.integration.error.IntegrationErrorCodes;
import io.agenticai.integration.error.IntegrationException;
import io.agenticai.integration.port.VersionDocumentsPort;
import io.agenticai.reg.contract.ServiceRegistry;
import io.agenticai.reg.contract.VersionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * {@link VersionDocumentsPort} over the Service Registry's published in-process interface
 * {@link ServiceRegistry} (CON-REG-009; REQ-INT-064, ADR-INT-020). One call to
 * {@code getServicePackageVersion(serviceCode, versionNumber)} with the Check's own service code and
 * version — never the current version — and the version's {@code requiredDocumentTypes} returned
 * exactly as stored, case-sensitive, in declared order, as an unmodifiable list. The rest of the
 * version (service knowledge, queries, document source) is dropped inside the call.
 *
 * <p>REG's {@link VersionNotFoundException} for the Check's own version (CON-REG-002 promises it
 * never disappears) is an integrity failure: {@code INT-500}, the refusal kept as cause (the advice
 * logs it with the Check identifier of the request). Injected only into the required-document-types
 * read service; it never receives the approval interface. Stateless (G9).
 */
@Component("intRegVersionDocumentsAdapter")
public class RegVersionDocumentsAdapter implements VersionDocumentsPort {

    private static final Logger log = LoggerFactory.getLogger(RegVersionDocumentsAdapter.class);

    private final ServiceRegistry serviceRegistry;

    public RegVersionDocumentsAdapter(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = Objects.requireNonNull(serviceRegistry, "serviceRegistry");
    }

    @Override
    public List<String> requiredDocumentTypes(String serviceCode, int versionNumber) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        try {
            return List.copyOf(serviceRegistry.getServicePackageVersion(serviceCode, versionNumber)
                    .requiredDocumentTypes());
        } catch (VersionNotFoundException notFound) {
            log.error("The registry holds no version {} of the service \"{}\" the Check runs on",
                    versionNumber, serviceCode);
            throw new IntegrationException(IntegrationErrorCodes.UNEXPECTED_FAILURE, notFound);
        }
    }
}
