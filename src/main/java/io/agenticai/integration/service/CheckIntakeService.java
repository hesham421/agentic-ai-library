package io.agenticai.integration.service;

import io.agenticai.chk.contract.StartedCheck;
import io.agenticai.integration.dto.StartCheckRequest;
import io.agenticai.integration.dto.StartedCheckResponse;
import io.agenticai.integration.port.CheckEnginePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * API-INT-001 — Start a Check (CON-INT-001). Parse → {@link CheckEnginePort#start} → answer at once;
 * the pipeline runs in the background inside the Check Engine (REQ-INT-002). The values are handed
 * on exactly as received (REQ-INT-003, REQ-INT-004); the Check Engine's refusals
 * ({@code CHK-400-START-INCOMPLETE}, {@code CHK-422-SERVICE-NOT-AVAILABLE},
 * {@code CHK-422-CONNECTION-NOT-ACTIVATED}) pass through unchanged (REQ-INT-006).
 *
 * <p>No {@code @Transactional}: INT owns no table (ADR-INT-015); the Check Engine's operation runs
 * its own transaction. Concurrency: none — every start is a new, independent Check.
 */
@Service("intCheckIntakeService")
public class CheckIntakeService {

    private static final Logger log = LoggerFactory.getLogger(CheckIntakeService.class);

    private final CheckEnginePort checkEngine;

    public CheckIntakeService(CheckEnginePort checkEngine) {
        this.checkEngine = Objects.requireNonNull(checkEngine, "checkEngine");
    }

    /** Starts a Check; answers its identifier, initial status and the address of its read. */
    public StartedCheckResponse start(StartCheckRequest request) {
        Objects.requireNonNull(request, "request");
        StartedCheck started = checkEngine.start(request.toCommand());
        log.info("INT check started checkId={} status={}", started.checkId(), started.status());
        return StartedCheckResponse.of(started);
    }
}
