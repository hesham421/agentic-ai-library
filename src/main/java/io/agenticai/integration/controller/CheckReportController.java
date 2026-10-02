package io.agenticai.integration.controller;

import io.agenticai.integration.dto.CheckReportResponse;
import io.agenticai.integration.dto.ChecksOfRequestResponse;
import io.agenticai.integration.service.CheckReportService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Host Integration's frontend-facing reads of the Report Store — a Check and its report
 * (API-INT-005) and the Checks of a request (API-INT-006), relayed unchanged (ADR-INT-020). They
 * sit under {@code /api/v1/check-reports}, distinct from the Report Store's own
 * {@code GET /api/v1/checks[/{checkId}]} (ADR-INT-020 (3)). Bean name {@code intCheckReportController}:
 * the Report Store has a controller of the same simple name. Plain JSON, no envelope; every error is
 * answered by {@code IntegrationProblemAdvice}. No authorization (raw-idea A2). Zero logic.
 *
 * <p>The API ids are held in constants and in the Javadoc, since springdoc (and so
 * {@code @Operation}) is not on the classpath.
 */
@RestController("intCheckReportController")
@RequestMapping("/api/v1/check-reports")
public class CheckReportController {

    /** API-INT-005 — Read a Check and its report. */
    public static final String API_INT_005 = "API-INT-005";

    /** API-INT-006 — List the Checks of a request. */
    public static final String API_INT_006 = "API-INT-006";

    private final CheckReportService checkReports;

    public CheckReportController(CheckReportService checkReports) {
        this.checkReports = Objects.requireNonNull(checkReports, "checkReports");
    }

    /**
     * {@value #API_INT_005} — {@code GET /api/v1/check-reports/{checkId}}: the Check and, once ended,
     * its report or failure and any recorded decision (200, {@code CheckReportResponse}). Errors:
     * INT-400-REQUEST-INVALID, RPT-404-CHECK-NOT-FOUND, INT-500. Honours CON-INT-005. Traces:
     * REQ-INT-006 … REQ-INT-008, REQ-INT-045 … REQ-INT-057, REQ-INT-061, REQ-INT-066.
     *
     * @param checkId DBF-INT-001, integer int64
     */
    @GetMapping("/{checkId}")
    public CheckReportResponse read(@PathVariable("checkId") Long checkId) {
        return checkReports.read(checkId);
    }

    /**
     * {@value #API_INT_006} — {@code GET /api/v1/check-reports?serviceCode=&requestNumber=}: at most
     * 100 Checks, newest first, with the total (200, {@code ChecksOfRequestResponse}). Errors:
     * RPT-400-REQUEST-KEYS-MISSING, INT-500. Honours CON-INT-006. Traces: REQ-INT-006, REQ-INT-008,
     * REQ-INT-040, REQ-INT-042, REQ-INT-043, REQ-INT-057, REQ-INT-062.
     *
     * <p>Both parameters are bound as optional: an absent one reaches the Report Store as
     * {@code null}, which refuses it with {@code RPT-400-REQUEST-KEYS-MISSING} — the operation's only
     * documented 400 (pass-through, ADR-INT-020 (6)). They are nonetheless documented as required
     * ({@code @Parameter(required = true)}), as the API document declares them: the binding is
     * optional only so that the refusal is the Report Store's own code.
     *
     * @param serviceCode   DBF-INT-003 (string ≤ 100), matched exactly as sent
     * @param requestNumber DBF-INT-005 (string ≤ 100), matched exactly as sent
     */
    @GetMapping
    public ChecksOfRequestResponse list(
            @Parameter(required = true) @RequestParam(name = "serviceCode", required = false) String serviceCode,
            @Parameter(required = true) @RequestParam(name = "requestNumber", required = false) String requestNumber) {
        return checkReports.list(serviceCode, requestNumber);
    }
}
