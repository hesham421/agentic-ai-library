package io.agenticai.integration.domain;

/**
 * The host Approval API definition of a service package version, as INT reads it through the
 * Service Registry's contract (CON-REG-012). The version supplies only whether the Approval API is
 * enabled, the HTTP method and the path; the base address is environment configuration
 * (ADR-INT-009, {@code aias.integration.approval.base-address}).
 *
 * @param enabled      DBF-INT-009 — whether the version enables the Approval API
 * @param method       the HTTP method, parsed from DBF-INT-010; {@code null} when not enabled
 * @param pathTemplate the path template, parsed from DBF-INT-010; {@code null} when not enabled
 */
public record ApprovalDefinition(boolean enabled, String method, String pathTemplate) {

    /** The definition of a version that does not enable the Approval API. */
    public static ApprovalDefinition disabled() {
        return new ApprovalDefinition(false, null, null);
    }
}
