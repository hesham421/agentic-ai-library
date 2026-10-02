package io.agenticai.rpt.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * The failure the Check Engine hands over with {@code failCheck}, and the decision whether it may
 * be stored (DATA-DOM RULE-RPT-010, owner layer domain): a failure reason, a detail that is not
 * blank and an end time not earlier than the start time → else
 * {@link RefusalReason#FAILURE_INCOMPLETE} naming the field. The failure reason's membership of
 * its closed list (RULE-RPT-006) is decided where the carried code is turned into
 * {@link CheckFailureReason} ({@code fromStored}); here an absent reason is an incomplete
 * failure. Whether the run may still fail at all (RULE-RPT-003) is
 * {@link CheckStatusTransition}'s.
 *
 * <p>Plain Java: no framework, no I/O; it returns a decision, the caller raises the SVC-API
 * exception.
 */
public final class CheckFailure {

    private final CheckFailureReason failureReason;
    private final String detail;
    private final OffsetDateTime endedAt;

    private CheckFailure(CheckFailureReason failureReason, String detail, OffsetDateTime endedAt) {
        this.failureReason = failureReason;
        this.detail = detail;
        this.endedAt = endedAt;
    }

    /** The failure as received; any value may be {@code null} — {@link #refusal(OffsetDateTime)} decides. */
    public static CheckFailure create(CheckFailureReason failureReason, String detail, OffsetDateTime endedAt) {
        return new CheckFailure(failureReason, detail, endedAt);
    }

    /**
     * The first missing value, or empty when the failure may be stored.
     *
     * @param startedAt the run's stored start time when the caller has it, else {@code null}: the
     *                  SVC-API {@code failCheck} reads no row before its conditional UPDATE, so
     *                  the end-after-start part is then backstopped by
     *                  {@code CHK_RPT_CHECK_RUN_ENDED_AT}
     */
    public Optional<RuleRefusal> refusal(OffsetDateTime startedAt) {
        if (failureReason == null) {
            return incomplete("failureReason");
        }
        if (Texts.isBlank(detail)) {
            return incomplete("detail");
        }
        if (endedAt == null || (startedAt != null && endedAt.isBefore(startedAt))) {
            return incomplete("endedAt");
        }
        return Optional.empty();
    }

    private static Optional<RuleRefusal> incomplete(String field) {
        return Optional.of(RuleRefusal.of(RefusalReason.FAILURE_INCOMPLETE, field));
    }

    public CheckFailureReason failureReason() {
        return failureReason;
    }

    public String detail() {
        return detail;
    }

    public OffsetDateTime endedAt() {
        return endedAt;
    }
}
