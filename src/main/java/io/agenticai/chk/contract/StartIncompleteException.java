package io.agenticai.chk.contract;

import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.error.CheckEngineTexts;

/**
 * REQ-CHK-004 — the service code, the request number or the employee identity of a Check to be
 * started is absent or blank; nothing is created (CON-CHK-004). In-process code
 * {@value CheckRejectionCodes#START_INCOMPLETE} (ADR-CHK-018).
 */
public class StartIncompleteException extends CheckEngineException {

    public StartIncompleteException() {
        super(CheckRejectionCodes.START_INCOMPLETE,
                CheckEngineTexts.english(CheckRejectionCodes.START_INCOMPLETE),
                null);
    }
}
