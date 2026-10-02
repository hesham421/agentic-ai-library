package io.agenticai.integration.port;

import io.agenticai.chk.contract.ConfirmedCheck;
import io.agenticai.chk.contract.StartedCheck;
import io.agenticai.integration.domain.StartCheckCommand;

/**
 * INT's port to the Check Engine (PORTS; REQ-INT-001, REQ-INT-018). The Check Engine's typed
 * refusals ({@code CHK-400-START-INCOMPLETE}, {@code CHK-422-SERVICE-NOT-AVAILABLE},
 * {@code CHK-422-CONNECTION-NOT-ACTIVATED}, {@code CHK-404-CHECK-NOT-FOUND},
 * {@code CHK-409-CHECK-NOT-AWAITING-DOCUMENTS}) propagate through it unchanged.
 */
public interface CheckEnginePort {

    /**
     * Starts a Check with the values exactly as received.
     *
     * @return the new Check's identifier and status ({@code RUNNING} or {@code AWAITING_DOCUMENTS})
     */
    StartedCheck start(StartCheckCommand command);

    /**
     * Confirms that the uploads of a {@code manual} Check are complete.
     *
     * @return the Check's identifier and status ({@code RUNNING})
     */
    ConfirmedCheck confirm(Long checkId);
}
