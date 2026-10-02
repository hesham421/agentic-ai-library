package io.agenticai.integration.adapter;

import io.agenticai.chk.contract.CheckEngine;
import io.agenticai.chk.contract.ConfirmedCheck;
import io.agenticai.chk.contract.StartedCheck;
import io.agenticai.integration.domain.StartCheckCommand;
import io.agenticai.integration.port.CheckEnginePort;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * {@link CheckEnginePort} over the Check Engine's published in-process interface
 * {@link CheckEngine} (PORTS; REQ-INT-001, REQ-INT-018). The values are passed exactly as received
 * — never trimmed or checked here. The Check Engine's typed refusals ({@code .contract}
 * exceptions) are not caught: they reach {@code IntegrationProblemAdvice} unchanged (REQ-INT-006,
 * ADR-INT-003). Stateless.
 */
@Component("intChkCheckEngineAdapter")
public class ChkCheckEngineAdapter implements CheckEnginePort {

    private final CheckEngine checkEngine;

    public ChkCheckEngineAdapter(CheckEngine checkEngine) {
        this.checkEngine = Objects.requireNonNull(checkEngine, "checkEngine");
    }

    @Override
    public StartedCheck start(StartCheckCommand command) {
        Objects.requireNonNull(command, "command");
        return checkEngine.startCheck(command.serviceCode(), command.requestNumber(), command.employeeId());
    }

    @Override
    public ConfirmedCheck confirm(Long checkId) {
        return checkEngine.confirmUploads(Objects.requireNonNull(checkId, "checkId"));
    }
}
