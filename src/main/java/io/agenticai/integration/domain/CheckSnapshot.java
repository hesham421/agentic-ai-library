package io.agenticai.integration.domain;

import java.util.Objects;

/**
 * What INT reads of one Check before it acts on it — built from the Report Store's read of the
 * Check (PORTS). Status and decision travel as their owners' codes (ADR-INT-013): INT owns no
 * lookup. Immutable; held only for the request (REQ-INT-059).
 *
 * @param checkId          DBF-INT-001 — the Check identifier
 * @param status           DBF-INT-002 — the Check status code (the Check Engine's closed list,
 *                         CON-CHK-001), e.g. {@code AWAITING_DOCUMENTS}, {@code COMPLETED}
 * @param serviceCode      DBF-INT-003 — the service code
 * @param versionNumber    DBF-INT-004 — the service package version number
 * @param requestNumber    DBF-INT-005 — the host's request number
 * @param employeeDecision DBF-INT-007 — the EMPLOYEE_DECISION code (APPROVED / REJECTED,
 *                         CON-RPT-002); {@code null} while the Check is undecided
 */
public record CheckSnapshot(
        Long checkId,
        String status,
        String serviceCode,
        Integer versionNumber,
        String requestNumber,
        String employeeDecision) {

    public CheckSnapshot {
        Objects.requireNonNull(checkId, "checkId");
        Objects.requireNonNull(status, "status");
    }

    /** Whether the Check already holds an Employee Decision. */
    public boolean decided() {
        return employeeDecision != null;
    }
}
