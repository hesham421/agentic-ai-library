package io.agenticai.reg.controller;

import io.agenticai.reg.dto.LoadResultResponse;
import io.agenticai.reg.dto.ServiceSummary;
import io.agenticai.reg.service.ServiceRegistryQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * The HTTP surface of the Service Registry (REG) — the three read operations of
 * {@code api-spec-reg.yaml}, and nothing else (REQ-REG-017, ADR-REG-007: no POST, PUT, PATCH or
 * DELETE). Plain JSON, no envelope; every error is answered by {@code ServiceRegistryProblemAdvice}
 * as a ProblemDetail — nothing is mapped here. No authorization: caller authentication is
 * deferred (raw-idea A2). Zero logic: every call is delegated as is.
 *
 * <p>The API id of each operation is held in a constant and in its Javadoc. springdoc serves this
 * controller in the {@code reg} group ({@code /v3/api-docs/reg}); the api-doc generator stamps each
 * served operation with its {@code x-api-id} from {@code api-spec-reg.yaml} by method and path, so
 * no {@code @Operation} annotation is needed here.
 */
@RestController
@RequestMapping("/api/v1")
public class ServiceRegistryController {

    /** API-REG-001 — List services. */
    public static final String API_REG_001 = "API-REG-001";
    /** API-REG-002 — Read one service. */
    public static final String API_REG_002 = "API-REG-002";
    /** API-REG-003 — Read the load report. */
    public static final String API_REG_003 = "API-REG-003";

    private final ServiceRegistryQueryService queryService;

    public ServiceRegistryController(ServiceRegistryQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    /**
     * {@value #API_REG_001} — {@code GET /api/v1/services}: the available services as
     * {@code ServiceSummary[]} (200). Errors: {@code REG-500}. Traces: REQ-REG-013, REQ-REG-030;
     * DBF-REG-002, 003, 007, 011, 016, 027. Honours CON-REG-008.
     */
    @GetMapping("/services")
    public List<ServiceSummary> listServices() {
        return queryService.listServices();
    }

    /**
     * {@value #API_REG_002} — {@code GET /api/v1/services/{serviceCode}}: the current version
     * summary of one service, available or withdrawn (200). Errors:
     * {@code REG-404-SERVICE-NOT-FOUND} (RULE-REG-016), {@code REG-500}. Traces: REQ-REG-014,
     * REQ-REG-015, REQ-REG-064; DBF-REG-002, 003, 007, 011, 016, 027. Honours CON-REG-010.
     *
     * @param serviceCode the service code, matched trimmed and case-insensitively (REQ-REG-064)
     */
    @GetMapping("/services/{serviceCode}")
    public ServiceSummary readService(@PathVariable("serviceCode") String serviceCode) {
        return queryService.getService(serviceCode);
    }

    /**
     * {@value #API_REG_003} — {@code GET /api/v1/load-results}: the Load Results of the latest
     * load run as {@code LoadResult[]} (200). Errors: {@code REG-500}. Traces: REQ-REG-008,
     * REQ-REG-007; DBF-REG-040 … DBF-REG-047.
     */
    @GetMapping("/load-results")
    public List<LoadResultResponse> readLoadReport() {
        return queryService.readLoadReport();
    }
}
