package io.agenticai.reg.contract;

/**
 * CON-REG-012 — the approval API definition of a stored version, exposed on its own interface so
 * that only INT's Employee Decision operation is given it (REQ-REG-044, REQ-REG-045; raw idea §12,
 * G2, AIAS-4). No other bean, and never the LLM, receives this interface.
 */
public interface ApprovalApiRegistry {

    /**
     * The approval API flag of the version and, only when enabled, its definition.
     *
     * @param serviceCode   the service code, matched trimmed and case-insensitively (REQ-REG-064)
     * @param versionNumber the version number the Check pinned (ADR-REG-020)
     * @throws VersionNotFoundException when no such service code or version is stored
     */
    ApprovalApi getApprovalApi(String serviceCode, int versionNumber);
}
