/**
 * Host Integration (INT): the one REST surface the host and the employee frontend call
 * (REQ-INT-057, ADR-INT-020), over the in-process interfaces of the Service Registry (REG),
 * Document Access (DOC), the Check Engine (CHK) and the Report Store (RPT).
 *
 * <p><b>Package naming.</b> The module code is {@code INT}, but {@code int} is a Java keyword and
 * cannot be a package name, so the module lives in {@code io.agenticai.integration} (sub-packages
 * {@code config}, {@code error}, {@code domain}, and later {@code port}, {@code adapter},
 * {@code service}, {@code dto}, {@code controller}). Error codes, messages and documentation keep
 * the {@code INT} code.
 *
 * <p><b>Design notes (CORE).</b>
 * <ul>
 *   <li><b>Module boundaries</b>: INT imports from another module only its {@code .contract}
 *       package ({@code io.agenticai.{reg,doc,chk,rpt}.contract}) and the platform
 *       ({@code io.agenticai.platform}). Refusals of CHK, DOC and RPT are passed through by
 *       {@link io.agenticai.integration.error.IntegrationProblemAdvice} with their own code, status
 *       and message (REQ-INT-006, ADR-INT-003).</li>
 *   <li><b>No state between requests</b> (REQ-INT-059): no table, cache, static or session field
 *       holds a request, a file, a report or a decision; the only in-memory structure is the
 *       per-Check approval lock of API-INT-004 (SVC-API), released when the request ends.</li>
 *   <li><b>No host database</b> (REQ-INT-060): INT declares no {@code DataSource}, JDBC template or
 *       MCP client; its only outbound host connection is the Approval API adapter (PORTS).</li>
 *   <li><b>No workflow engine</b>; no server-rendered page or view controller (REQ-INT-058).</li>
 *   <li><b>No lookup of its own</b> (ADR-INT-013): Check status and Employee Decision travel as the
 *       owners' codes (strings).</li>
 * </ul>
 */
package io.agenticai.integration;
