package io.agenticai.integration.dto;

/**
 * The API document's {@code UploadConfirmationRequest} schema (API-INT-003): an empty object — the
 * confirmation carries no field. The body must still be readable JSON (INT-400-REQUEST-INVALID
 * otherwise, REQ-INT-007).
 */
public record UploadConfirmationRequest() {
}
