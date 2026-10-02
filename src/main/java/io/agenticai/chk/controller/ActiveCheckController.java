package io.agenticai.chk.controller;

import io.agenticai.chk.dto.ActiveCheckView;
import io.agenticai.chk.service.ActiveCheckQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * The HTTP surface of the Check Engine (CHK) — the one read operation of {@code api-spec-chk.yaml},
 * and nothing else (ADR-CHK-017: no POST, PUT, PATCH or DELETE). Plain JSON, no envelope; every
 * error is answered by {@code CheckEngineProblemAdvice} as a ProblemDetail — nothing is mapped
 * here. No authorization: caller authentication is deferred (raw-idea A2). Zero logic.
 *
 * <p>The API id is held in a constant and in the Javadoc, since springdoc (and so
 * {@code @Operation}) is not on the classpath.
 */
@RestController
@RequestMapping("/api/v1/active-checks")
public class ActiveCheckController {

    /** API-CHK-001 — Read the Active Check of a Check. */
    public static final String API_CHK_001 = "API-CHK-001";

    private final ActiveCheckQueryService queryService;

    public ActiveCheckController(ActiveCheckQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    /**
     * {@value #API_CHK_001} — {@code GET /api/v1/active-checks/{checkId}}: the status and deadline
     * of the unfinished Check (200, {@code ActiveCheckView}). Errors: {@code CHK-400-CHECK-ID-INVALID}
     * — {@code checkId} not a number, raised by Spring MVC's binding and mapped by the advice;
     * {@code CHK-404-ACTIVE-CHECK-NOT-FOUND} — the Check has ended or does not exist;
     * {@code CHK-500}. Traces: REQ-CHK-076, REQ-CHK-077, REQ-CHK-080; DBF-CHK-002, 003, 004.
     *
     * @param checkId the Check identifier (DBF-CHK-002), integer int64
     */
    @GetMapping("/{checkId}")
    public ActiveCheckView readActiveCheck(@PathVariable("checkId") Long checkId) {
        return queryService.findActiveCheck(checkId);
    }
}
