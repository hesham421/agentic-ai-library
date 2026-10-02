package io.agenticai.reg.contract;

/**
 * CON-REG-012 — the approval API of a stored version as supplied to the Employee Decision path
 * (REQ-REG-044, REQ-REG-045).
 *
 * @param approvalEnabled whether the version enables the approval API
 * @param approvalApi     the approval API definition (method + path pattern) — present only when
 *                        enabled, {@code null} otherwise
 */
public record ApprovalApi(boolean approvalEnabled, String approvalApi) {
}
