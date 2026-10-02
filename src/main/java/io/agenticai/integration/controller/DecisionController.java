package io.agenticai.integration.controller;

import io.agenticai.integration.dto.DecisionRequest;
import io.agenticai.integration.dto.RecordedDecisionResponse;
import io.agenticai.integration.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Host Integration's Employee Decision — {@code POST /api/v1/checks/{checkId}/decision}
 * (API-INT-004); the only endpoint that carries a decision (REQ-INT-024). Plain JSON, no envelope;
 * every error is answered by {@code IntegrationProblemAdvice} as a ProblemDetail — nothing is
 * mapped here. No authorization (raw-idea A2). Zero logic: whether the host Approval API is called
 * is decided by {@link DecisionService}.
 *
 * <p>The API id is held in a constant and in the Javadoc, since springdoc (and so
 * {@code @Operation}) is not on the classpath.
 */
@RestController("intDecisionController")
@RequestMapping("/api/v1/checks/{checkId}")
public class DecisionController {

    /** API-INT-004 — Record an Employee Decision. */
    public static final String API_INT_004 = "API-INT-004";

    private final DecisionService decisions;

    public DecisionController(DecisionService decisions) {
        this.decisions = Objects.requireNonNull(decisions, "decisions");
    }

    /**
     * {@value #API_INT_004} — {@code POST /api/v1/checks/{checkId}/decision}: records the Employee
     * Decision, through the host Approval API where the decision is APPROVED and the Check's version
     * enables it (201, {@code RecordedDecisionResponse}). Errors: INT-400-REQUEST-INVALID,
     * RPT-404-CHECK-NOT-FOUND, RPT-400-DECISION-INCOMPLETE, RPT-409-CHECK-NOT-COMPLETED,
     * RPT-409-DECISION-ALREADY-RECORDED, RPT-422-APPROVAL-FLAG-ON-REJECTION,
     * INT-502-APPROVAL-API-FAILED, INT-504-APPROVAL-API-TIMED-OUT, INT-500. Honours CON-INT-004.
     * Traces: REQ-INT-006 … REQ-INT-008, REQ-INT-021 … REQ-INT-039, REQ-INT-054.
     *
     * @param checkId  DBF-INT-001, integer int64
     * @param decision the decision, exactly as received
     */
    @PostMapping("/decision")
    @ResponseStatus(HttpStatus.CREATED)
    public RecordedDecisionResponse decide(@PathVariable("checkId") Long checkId,
                                           @Valid @RequestBody DecisionRequest decision) {
        return decisions.decide(checkId, decision);
    }
}
