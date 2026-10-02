package io.agenticai.rpt.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * The opening of a Check run — the values {@code createCheckRun} receives, and the decision
 * whether they may be stored (DATA-DOM ENT-RPT-001, owner layer domain):
 *
 * <ol>
 *   <li>RULE-RPT-001 — every value present and not blank → else
 *       {@link RefusalReason#CHECK_RUN_INCOMPLETE} naming the first missing field;</li>
 *   <li>RULE-RPT-006 — the initial status is a creation code: {@code AWAITING_DOCUMENTS} or
 *       {@code RUNNING} ("status not RUNNING / AWAITING_DOCUMENTS at creation counts as outside",
 *       SVC-API) → else {@link RefusalReason#UNKNOWN_CODE} for {@code CHECK_STATUS};</li>
 *   <li>RULE-RPT-002 — {@code AWAITING_DOCUMENTS} only with {@code manual}
 *       ({@link RefusalReason#AWAITING_NEEDS_MANUAL}), and {@code manual} only with
 *       {@code AWAITING_DOCUMENTS} ({@link RefusalReason#MANUAL_STARTS_AWAITING}).</li>
 * </ol>
 *
 * The order is the SVC-API order. A code that is not a member of its closed list at all is
 * caught where the carried code is turned into the RPT enum ({@code fromStored} empty →
 * RULE-RPT-006); here a {@code null} enum is an absent value (RULE-RPT-001).
 *
 * <p>Plain Java: no framework, no I/O; it returns a decision, it does not throw — the caller
 * raises the SVC-API exception of the reason and, when nothing is refused, stores the run.
 */
public final class CheckRunOpening {

    private final String serviceCode;
    private final Integer versionNumber;
    private final FetchMode fetchMode;
    private final String requestNumber;
    private final String employeeId;
    private final CheckStatus status;
    private final OffsetDateTime startedAt;

    private CheckRunOpening(String serviceCode, Integer versionNumber, FetchMode fetchMode,
                            String requestNumber, String employeeId, CheckStatus status,
                            OffsetDateTime startedAt) {
        this.serviceCode = serviceCode;
        this.versionNumber = versionNumber;
        this.fetchMode = fetchMode;
        this.requestNumber = requestNumber;
        this.employeeId = employeeId;
        this.status = status;
        this.startedAt = startedAt;
    }

    /** The values as received; any may be {@code null} — {@link #refusal()} decides. */
    public static CheckRunOpening create(String serviceCode, Integer versionNumber, FetchMode fetchMode,
                                         String requestNumber, String employeeId, CheckStatus status,
                                         OffsetDateTime startedAt) {
        return new CheckRunOpening(serviceCode, versionNumber, fetchMode, requestNumber, employeeId,
                status, startedAt);
    }

    /** The first rule the values break, or empty when the run may be stored. */
    public Optional<RuleRefusal> refusal() {
        Optional<String> missing = firstMissingField();
        if (missing.isPresent()) {
            return Optional.of(RuleRefusal.of(RefusalReason.CHECK_RUN_INCOMPLETE, missing.get()));
        }
        if (status != CheckStatus.AWAITING_DOCUMENTS && status != CheckStatus.RUNNING) {
            return Optional.of(RuleRefusal.of(RefusalReason.UNKNOWN_CODE,
                    status.storedValue(), "CHECK_STATUS"));
        }
        if (status == CheckStatus.AWAITING_DOCUMENTS && fetchMode != FetchMode.MANUAL) {
            return Optional.of(RuleRefusal.of(RefusalReason.AWAITING_NEEDS_MANUAL));
        }
        if (fetchMode == FetchMode.MANUAL && status != CheckStatus.AWAITING_DOCUMENTS) {
            return Optional.of(RuleRefusal.of(RefusalReason.MANUAL_STARTS_AWAITING));
        }
        return Optional.empty();
    }

    private Optional<String> firstMissingField() {
        if (Texts.isBlank(serviceCode)) {
            return Optional.of("serviceCode");
        }
        if (versionNumber == null) {
            return Optional.of("versionNumber");
        }
        if (fetchMode == null) {
            return Optional.of("fetchMode");
        }
        if (Texts.isBlank(requestNumber)) {
            return Optional.of("requestNumber");
        }
        if (Texts.isBlank(employeeId)) {
            return Optional.of("employeeId");
        }
        if (status == null) {
            return Optional.of("status");
        }
        if (startedAt == null) {
            return Optional.of("startedAt");
        }
        return Optional.empty();
    }

    public String serviceCode() {
        return serviceCode;
    }

    public Integer versionNumber() {
        return versionNumber;
    }

    public FetchMode fetchMode() {
        return fetchMode;
    }

    public String requestNumber() {
        return requestNumber;
    }

    public String employeeId() {
        return employeeId;
    }

    public CheckStatus status() {
        return status;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }
}
