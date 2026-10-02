package io.agenticai.rpt.controller;

import io.agenticai.rpt.contract.AgreementRow;
import io.agenticai.rpt.contract.CheckReport;
import io.agenticai.rpt.contract.ChecksOfRequest;
import io.agenticai.rpt.service.CheckReportQueryService;
import io.agenticai.rpt.service.DecisionAgreementQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * The HTTP surface of the Report Store (RPT) — the three read operations of
 * {@code api-spec-rpt.yaml}, and nothing else (ADR-RPT-006: no POST, PUT, PATCH or DELETE; every
 * write is in-process). Plain JSON, no envelope; every value is returned exactly as stored, as
 * string data — no HTML, no template (REQ-RPT-027). Every error is answered by
 * {@code ReportStoreProblemAdvice} as a ProblemDetail — nothing is mapped here: a non-numeric
 * {@code checkId} (RPT-400-CHECK-ID-INVALID) and an absent query parameter
 * (RPT-400-REQUEST-KEYS-MISSING / RPT-400-SERVICE-CODE-MISSING) are raised by Spring MVC's binding,
 * a blank one by the service. No authorization: caller authentication is deferred (raw-idea A2).
 * Zero logic.
 *
 * <p>The API ids are held in constants and in the Javadoc, since springdoc (and so
 * {@code @Operation}) is not on the classpath. The response records are the
 * {@code io.agenticai.rpt.contract} records, which match the API document's schemas field for field.
 */
@RestController
@RequestMapping("/api/v1")
public class CheckReportController {

    /** API-RPT-001 — Read a Check and its report. */
    public static final String API_RPT_001 = "API-RPT-001";

    /** API-RPT-002 — List the Checks of a request. */
    public static final String API_RPT_002 = "API-RPT-002";

    /** API-RPT-003 — Read the decision agreement of a service. */
    public static final String API_RPT_003 = "API-RPT-003";

    private final CheckReportQueryService checkReports;
    private final DecisionAgreementQueryService decisionAgreement;

    public CheckReportController(CheckReportQueryService checkReports,
                                 DecisionAgreementQueryService decisionAgreement) {
        this.checkReports = Objects.requireNonNull(checkReports, "checkReports");
        this.decisionAgreement = Objects.requireNonNull(decisionAgreement, "decisionAgreement");
    }

    /**
     * {@value #API_RPT_001} — {@code GET /api/v1/checks/{checkId}}: the Check and, once COMPLETED,
     * its report (200, {@code CheckReport}). Errors: RPT-400-CHECK-ID-INVALID, RPT-404-CHECK-NOT-FOUND
     * (also a purged Check), RPT-500. Honours CON-RPT-003, CON-RPT-001. Traces: REQ-RPT-023 …
     * REQ-RPT-027.
     *
     * @param checkId the Check identifier (DBF-RPT-001), integer int64
     */
    @GetMapping("/checks/{checkId}")
    public CheckReport readCheck(@PathVariable("checkId") Long checkId) {
        return checkReports.read(checkId);
    }

    /**
     * {@value #API_RPT_002} — {@code GET /api/v1/checks?serviceCode=&requestNumber=}: at most 100
     * Checks, newest first, with the total (200, {@code ChecksOfRequest}). Errors:
     * RPT-400-REQUEST-KEYS-MISSING, RPT-500. Honours CON-RPT-004. Traces: REQ-RPT-028 … REQ-RPT-031,
     * REQ-RPT-050.
     *
     * @param serviceCode   DBF-RPT-002, matched exactly
     * @param requestNumber DBF-RPT-005, matched exactly as sent
     */
    @GetMapping("/checks")
    public ChecksOfRequest listChecksOfRequest(@RequestParam("serviceCode") String serviceCode,
                                               @RequestParam("requestNumber") String requestNumber) {
        return checkReports.listOfRequest(serviceCode, requestNumber);
    }

    /**
     * {@value #API_RPT_003} — {@code GET /api/v1/decision-agreement?serviceCode=}: counts of decided
     * Checks per version, Overall Status and Employee Decision (200, array of {@code AgreementRow}).
     * Errors: RPT-400-SERVICE-CODE-MISSING, RPT-500. Honours CON-RPT-005, CON-RPT-002. Traces:
     * REQ-RPT-040, REQ-RPT-041.
     *
     * @param serviceCode DBF-RPT-002
     */
    @GetMapping("/decision-agreement")
    public List<AgreementRow> readDecisionAgreement(@RequestParam("serviceCode") String serviceCode) {
        return decisionAgreement.read(serviceCode);
    }
}
