package io.agenticai.integration.controller;

import io.agenticai.integration.dto.StartCheckRequest;
import io.agenticai.integration.dto.StartedCheckResponse;
import io.agenticai.integration.service.CheckIntakeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;

/**
 * Host Integration's start of a Check — {@code POST /api/v1/checks} (API-INT-001). Plain JSON, no
 * envelope; every error is answered by {@code IntegrationProblemAdvice} as a ProblemDetail — nothing
 * is mapped here. No authorization (raw-idea A2). Zero logic.
 *
 * <p>Only POST is mapped on {@code /api/v1/checks}; the Report Store owns {@code GET} on the same
 * path (API-RPT-002), so the two never overlap. The API id is held in a constant and in the Javadoc.
 */
@RestController("intCheckIntakeController")
@RequestMapping("/api/v1/checks")
public class CheckIntakeController {

    /** API-INT-001 — Start a Check. */
    public static final String API_INT_001 = "API-INT-001";

    private final CheckIntakeService intake;

    public CheckIntakeController(CheckIntakeService intake) {
        this.intake = Objects.requireNonNull(intake, "intake");
    }

    /**
     * {@value #API_INT_001} — {@code POST /api/v1/checks}: starts a Check and answers at once
     * (202, {@code StartedCheckResponse}, header {@code Location: /api/v1/checks/{checkId}}).
     * Errors: INT-400-REQUEST-INVALID, CHK-400-START-INCOMPLETE, CHK-422-SERVICE-NOT-AVAILABLE,
     * CHK-422-CONNECTION-NOT-ACTIVATED, INT-500. Honours CON-INT-001. Traces: REQ-INT-001 …
     * REQ-INT-008, REQ-INT-044, REQ-INT-057 … REQ-INT-060.
     *
     * <p>{@code @ResponseStatus(ACCEPTED)} repeats the status the {@code ResponseEntity} already
     * sets, so the served OpenAPI (springdoc reads the annotation, not the returned entity)
     * documents 202 as the API document does; the runtime answer is unchanged.
     *
     * @param request the start, exactly as received
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<StartedCheckResponse> start(@Valid @RequestBody StartCheckRequest request) {
        StartedCheckResponse started = intake.start(request);
        return ResponseEntity.accepted().location(URI.create(started.checkUrl())).body(started);
    }
}
