package io.agenticai.integration.dto;

import io.agenticai.integration.domain.StartCheckCommand;

/**
 * The API document's {@code StartCheckRequest} schema (API-INT-001), field for field. Every value
 * is passed to the Check Engine exactly as received — never trimmed, never checked against a
 * directory (REQ-INT-003, REQ-INT-004).
 *
 * <p><b>No bean-validation constraint, by design.</b> The document marks the three fields
 * required (≤ 100 characters), but the unit (SVC-API-COMMAND, API-INT-001) states that presence is
 * decided by the Check Engine: an absent or blank value must answer
 * {@code CHK-400-START-INCOMPLETE} with the Check Engine's own text (pass-through, REQ-INT-006). A
 * {@code @NotBlank} here would answer a different code first; the length is the Report Store's
 * column limit. Recorded as an {@code api_doc_gaps} row.
 *
 * @param serviceCode   DBF-INT-003 — the service code, string ≤ 100, required
 * @param requestNumber DBF-INT-005 — the host's request number, exactly as sent, string ≤ 100, required
 * @param employeeId    DBF-INT-006 — the employee's identity, exactly as sent, string ≤ 100, required
 */
public record StartCheckRequest(String serviceCode, String requestNumber, String employeeId) {

    /** The command handed to the Check Engine port — the values unchanged. */
    public StartCheckCommand toCommand() {
        return new StartCheckCommand(serviceCode, requestNumber, employeeId);
    }
}
