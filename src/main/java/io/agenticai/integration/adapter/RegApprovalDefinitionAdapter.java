package io.agenticai.integration.adapter;

import io.agenticai.integration.domain.ApprovalDefinition;
import io.agenticai.integration.error.IntegrationErrorCodes;
import io.agenticai.integration.error.IntegrationException;
import io.agenticai.integration.port.ApprovalDefinitionPort;
import io.agenticai.reg.contract.ApprovalApi;
import io.agenticai.reg.contract.ApprovalApiRegistry;
import io.agenticai.reg.contract.VersionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * {@link ApprovalDefinitionPort} over the Service Registry's approval interface
 * {@link ApprovalApiRegistry} — the separate interface REG gives only to the Employee Decision
 * operation (CON-REG-012; REQ-INT-025, REQ-INT-028, REQ-INT-030; AIAS-4). Injected only into the
 * decision service and never exposed to a model.
 *
 * <p>One call to {@code getApprovalApi(serviceCode, versionNumber)} with the Check's own service
 * code and version (REQ-INT-030 — the version the Check ran on, not the current one). Mapping:
 * <ul>
 *   <li>{@code approvalEnabled} (DBF-INT-009) false → {@link ApprovalDefinition#disabled()}; the
 *       definition text is then ignored.</li>
 *   <li>enabled → {@code approvalApi} (DBF-INT-010, e.g. {@code POST /requests/{requestId}/approve})
 *       split at its first whitespace into {@code method} and {@code pathTemplate}.</li>
 * </ul>
 *
 * <p>Failures — both are integrity failures of what REG stored, answered {@code INT-500} (the
 * advice logs it with the Check identifier of the request) with the original kept as the cause:
 * <ul>
 *   <li>REG's {@link VersionNotFoundException} for the Check's own version — CON-REG-002 promises
 *       a stored version never disappears;</li>
 *   <li>an enabled definition that cannot be called: blank, no whitespace between method and path,
 *       a method that is not an upper-case token, a path not starting with {@code /}, or a path
 *       without exactly one {@code {…}} placeholder for the request number (REG checks only that an
 *       enabled definition is present — RULE-REG-011). It is never treated as "disabled": an
 *       APPROVED decision would then be recorded as not executed through an Approval API the
 *       version enables.</li>
 * </ul>
 * Stateless: nothing is kept beyond the call; no cache (G9).
 */
@Component("intRegApprovalDefinitionAdapter")
public class RegApprovalDefinitionAdapter implements ApprovalDefinitionPort {

    private static final Logger log = LoggerFactory.getLogger(RegApprovalDefinitionAdapter.class);

    private static final Pattern METHOD = Pattern.compile("[A-Z]+");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^{}/]+}");

    private final ApprovalApiRegistry approvalApiRegistry;

    public RegApprovalDefinitionAdapter(ApprovalApiRegistry approvalApiRegistry) {
        this.approvalApiRegistry = Objects.requireNonNull(approvalApiRegistry, "approvalApiRegistry");
    }

    @Override
    public ApprovalDefinition definitionOf(String serviceCode, int versionNumber) {
        Objects.requireNonNull(serviceCode, "serviceCode");
        ApprovalApi approvalApi;
        try {
            approvalApi = approvalApiRegistry.getApprovalApi(serviceCode, versionNumber);
        } catch (VersionNotFoundException notFound) {
            log.error("The registry holds no version {} of the service \"{}\" the Check ran on",
                    versionNumber, serviceCode);
            throw new IntegrationException(IntegrationErrorCodes.UNEXPECTED_FAILURE, notFound);
        }
        if (!approvalApi.approvalEnabled()) {
            return ApprovalDefinition.disabled();
        }
        return parse(approvalApi.approvalApi(), serviceCode, versionNumber);
    }

    private static ApprovalDefinition parse(String text, String serviceCode, int versionNumber) {
        String definition = text == null ? "" : text.strip();
        int space = firstWhitespace(definition);
        String method = space < 0 ? "" : definition.substring(0, space);
        String pathTemplate = space < 0 ? "" : definition.substring(space + 1).strip();
        if (!METHOD.matcher(method).matches()
                || !pathTemplate.startsWith("/")
                || PLACEHOLDER.matcher(pathTemplate).results().count() != 1
                || pathTemplate.chars().filter(c -> c == '{').count() != 1) {
            log.error("The Approval API definition of version {} of the service \"{}\" cannot be called: \"{}\"",
                    versionNumber, serviceCode, definition);
            throw new IntegrationException(IntegrationErrorCodes.UNEXPECTED_FAILURE,
                    new IllegalStateException("Malformed Approval API definition of version " + versionNumber
                            + " of the service \"" + serviceCode + "\""));
        }
        return new ApprovalDefinition(true, method, pathTemplate);
    }

    private static int firstWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
