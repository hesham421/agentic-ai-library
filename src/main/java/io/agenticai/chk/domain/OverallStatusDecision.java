package io.agenticai.chk.domain;

import java.util.Collection;
import java.util.Objects;

/**
 * The Overall Status of a Check (REQ-CHK-041 … REQ-CHK-043, ADR-CHK-002): NOT_COMPLIANT when any
 * finding is NOT_SATISFIED; otherwise NEEDS_MANUAL_REVIEW when any finding is UNDETERMINED or any
 * service query was not read; otherwise COMPLIANT.
 *
 * <p>Plain Java, no state: one static operation.
 */
public final class OverallStatusDecision {

    private OverallStatusDecision() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * @param outcomes       the outcomes of every finding of the Check
     * @param anyQueryUnread whether at least one service query was not read
     */
    public static OverallStatus decide(Collection<FindingOutcome> outcomes, boolean anyQueryUnread) {
        Objects.requireNonNull(outcomes, "outcomes");
        if (outcomes.contains(FindingOutcome.NOT_SATISFIED)) {
            return OverallStatus.NOT_COMPLIANT;
        }
        if (anyQueryUnread || outcomes.contains(FindingOutcome.UNDETERMINED)) {
            return OverallStatus.NEEDS_MANUAL_REVIEW;
        }
        return OverallStatus.COMPLIANT;
    }
}
