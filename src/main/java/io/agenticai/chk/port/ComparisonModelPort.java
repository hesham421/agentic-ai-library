package io.agenticai.chk.port;

import java.time.Instant;

/**
 * The Check Engine's only way to the comparison model (REQ-CHK-030 … REQ-CHK-037, REQ-CHK-064,
 * REQ-CHK-067): one call per Check, given the service knowledge of the Check's version and the
 * Check's own data, answering the model's findings in the fixed report structure. The answer is
 * only data — nothing in it is ever executed (REQ-CHK-031) — and the findings are verified in
 * code afterwards (ADR-CHK-003). No implementation of this port is ever handed to a model as a
 * tool, and an implementation gives the model none (REQ-CHK-030).
 *
 * <p>The plan's signature {@code compare(serviceKnowledge, CheckData)} carries one more argument,
 * the Check's deadline, so that the call is bounded by the time left before the Check timeout,
 * like every other external call of a Check (REQ-CHK-016, REQ-CHK-051, guardrail G8).
 */
public interface ComparisonModelPort {

    /**
     * Compares the Check's data with the service knowledge.
     *
     * @param serviceKnowledge the whole service knowledge of the Check's version, unaltered — the
     *                         only service instructions the model receives (REQ-CHK-034)
     * @param data             the Check's query results and documents — passed to the model as
     *                         delimited data only (REQ-CHK-035, REQ-CHK-036)
     * @param deadline         the instant by which the Check must end
     * @return the model's findings, parsed into the fixed report structure (REQ-CHK-037)
     * @throws ModelNotPermittedException  the model's tier is FREE and the environment's data
     *                                     class is REAL — nothing was sent (REQ-CHK-072,
     *                                     REQ-CHK-073)
     * @throws ModelUnavailableException   no comparison model is configured, the provider could
     *                                     not be reached, answered an error or gave no answer
     *                                     (REQ-CHK-053)
     * @throws ComparisonTimedOutException the deadline was reached before or during the call
     *                                     (REQ-CHK-051)
     * @throws ModelOutputInvalidException the answer does not fit the fixed report structure
     *                                     (REQ-CHK-039)
     */
    ComparisonOutput compare(String serviceKnowledge, CheckData data, Instant deadline);
}
