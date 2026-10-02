package io.agenticai.rpt.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * RULE-RPT-003 — the Check status moves forward only (DATA-DOM STATE MACHINE, owner layer
 * domain). Allowed: {@code AWAITING_DOCUMENTS → RUNNING}, {@code RUNNING → RUNNING} (mark
 * running), {@code RUNNING → COMPLETED} (complete), {@code AWAITING_DOCUMENTS | RUNNING → FAILED}
 * (fail); every change from {@code COMPLETED} or {@code FAILED} is refused.
 *
 * <p>Two places use it: the completion, which loads the run {@code FOR UPDATE} and decides
 * before any write; and the diagnosis of a conditional UPDATE that changed 0 rows (mark running,
 * fail — the repository guard of RULE-RPT-003), which reads the run and asks why. Plain Java: no
 * framework, no I/O; it returns a decision, the caller raises the SVC-API exception.
 */
public final class CheckStatusTransition {

    private final CheckStatus current;

    private CheckStatusTransition(CheckStatus current) {
        this.current = current;
    }

    /** The transitions open from the stored status. */
    public static CheckStatusTransition from(CheckStatus current) {
        return new CheckStatusTransition(Objects.requireNonNull(current, "current"));
    }

    /** {@code true} when the Check has ended — COMPLETED and FAILED are terminal. */
    public boolean isEnded() {
        return current == CheckStatus.COMPLETED || current == CheckStatus.FAILED;
    }

    /** Mark running: refused with {@link RefusalReason#CHECK_ENDED} once ended. */
    public Optional<RuleRefusal> refusalToMarkRunning() {
        return isEnded() ? Optional.of(RuleRefusal.of(RefusalReason.CHECK_ENDED)) : Optional.empty();
    }

    /**
     * Complete: refused with {@link RefusalReason#CHECK_ENDED} once ended, with
     * {@link RefusalReason#CHECK_NOT_RUNNING} while {@code AWAITING_DOCUMENTS}.
     */
    public Optional<RuleRefusal> refusalToComplete() {
        if (isEnded()) {
            return Optional.of(RuleRefusal.of(RefusalReason.CHECK_ENDED));
        }
        if (current != CheckStatus.RUNNING) {
            return Optional.of(RuleRefusal.of(RefusalReason.CHECK_NOT_RUNNING));
        }
        return Optional.empty();
    }

    /** Fail: refused with {@link RefusalReason#CHECK_ENDED} once ended. */
    public Optional<RuleRefusal> refusalToFail() {
        return refusalToMarkRunning();
    }
}
