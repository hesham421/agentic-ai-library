package io.agenticai.integration.domain;

/**
 * The start of a Check as API-INT-001 received it — every value exactly as received, neither
 * trimmed nor validated here: the Check Engine refuses absent or blank values itself
 * ({@code CHK-400-START-INCOMPLETE}, passed through — REQ-INT-006).
 *
 * @param serviceCode   DBF-INT-003 — the service code
 * @param requestNumber DBF-INT-005 — the host's request number
 * @param employeeId    DBF-INT-006 — the employee's identity
 */
public record StartCheckCommand(String serviceCode, String requestNumber, String employeeId) {
}
