/**
 * The Check Engine (CHK): runs one Check of one request against one service package version and
 * hands its result to the Check result port.
 *
 * <p><b>Design notes (CORE).</b>
 * <ul>
 *   <li><b>No workflow engine</b> (REQ-CHK-009): the pipeline is plain sequential service code —
 *       no workflow, BPM or state-machine framework.</li>
 *   <li><b>No state between Checks</b> (REQ-CHK-063 … REQ-CHK-065, guardrail G9): no cache,
 *       static field, conversation memory or session holds a query result, document content or
 *       model output. A pipeline's working data is a local {@code CheckContext} object dropped
 *       when the pipeline returns; the only stored data is {@code CHK_ACTIVE_CHECK}, which holds no
 *       request data (REQ-CHK-082).</li>
 *   <li><b>Module boundaries</b>: CHK reaches REG only through {@code io.agenticai.reg.contract}
 *       and DOC only through {@code io.agenticai.doc.contract}; the per-Check limits come from the
 *       platform's {@link io.agenticai.platform.config.CheckLimitsProperties}.</li>
 * </ul>
 */
package io.agenticai.chk;
