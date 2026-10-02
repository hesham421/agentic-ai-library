package io.agenticai.platform.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * The platform-wide per-Check limits, bound from {@code aias.check.*} (AIAS-7: every Check
 * declares its timeout, maximum rows and maximum file size). Owned by the platform track, not by
 * a module: the profile's {@code conventions.module_interface} is {@code in_process} — one
 * deployable — and both DOC's CORE and CHK's CORE name the {@code aias.check.*} group "for every
 * module", so this is the only {@code @ConfigurationProperties} class of the prefix (CU.6). It is
 * consumed by Document Access (DOC) and the Check Engine (CHK); neither module defines a second
 * class for these keys. The limits are platform configuration — never read from a service
 * definition (RULE-REG-012, ADR-REG-006, AIAS-7).
 *
 * <p>Keys and defaults: {@code timeout}, {@code max-rows}, {@code max-file-size} (ADR-REG-019);
 * {@code max-uploads} (ADR-DOC-016); {@code upload-window}, {@code deadline-check-interval},
 * {@code pipeline-threads} (CHK CORE, REQ-CHK-060, ADR-CHK-004, REQ-CHK-080, REQ-CHK-001). The
 * nested {@code aias.check.comparison-model.*} group is CHK's own, bound by CHK's
 * {@code ComparisonModelProperties}; this record deliberately declares no component for it.
 *
 * <p>The limits must exist for every Check, so the record is {@code @Validated}: each value with a
 * stated default is defaulted when absent, and every value is refused when not positive, failing
 * start-up with the reason rather than running a Check without a limit (raw-idea §12 guardrail 8).
 * {@code upload-window} has no default stated by any plan, SRS requirement or decision
 * (CHK CORE, REQ-CHK-060, ADR-CHK-004), so it is <b>required</b>: start-up fails until the
 * environment sets {@code aias.check.upload-window} (recorded as an open API-document gap).
 *
 * <p>DOC applies the timeout, the maximum rows and the maximum file size on every fetch
 * (REQ-DOC-040, REQ-DOC-039, REQ-DOC-042) and the maximum uploads on every handover
 * (REQ-DOC-063, ADR-DOC-016); CHK applies the timeout and the maximum rows on every Check
 * (REQ-CHK-051, REQ-CHK-016), the upload window to every {@code manual} Check (REQ-CHK-060), the
 * deadline-check interval to its deadline check (REQ-CHK-080) and the pipeline threads to its
 * background executor (REQ-CHK-001).
 *
 * @param timeout               the Check's timeout, counted from RUNNING (REQ-DOC-040,
 *                              REQ-CHK-051); default {@code PT2M}
 * @param maxRows               the maximum rows a query may return (REQ-DOC-039, REQ-CHK-016);
 *                              default {@code 100}
 * @param maxFileSize           the maximum size of one document (REQ-DOC-042); default {@code 10MB}
 * @param maxUploads            the maximum Uploaded Documents per Check (REQ-DOC-063); default
 *                              {@code 20}
 * @param uploadWindow          how long a {@code manual} Check may stay AWAITING_DOCUMENTS
 *                              (REQ-CHK-060, ADR-CHK-004); required — no stated default
 * @param deadlineCheckInterval the period of CHK's deadline check (REQ-CHK-080); default
 *                              {@code PT30S}
 * @param pipelineThreads       the size of CHK's bounded background pipeline executor
 *                              (REQ-CHK-001); default {@code 4}
 */
@Validated
@ConfigurationProperties(prefix = "aias.check")
public record CheckLimitsProperties(
        @NotNull Duration timeout,
        @NotNull @Positive Integer maxRows,
        @NotNull DataSize maxFileSize,
        @NotNull @Positive Integer maxUploads,
        @NotNull Duration uploadWindow,
        @NotNull Duration deadlineCheckInterval,
        @NotNull @Positive Integer pipelineThreads) {

    /** Default of {@code aias.check.timeout} (ADR-REG-019). */
    public static final Duration DEFAULT_TIMEOUT = Duration.parse("PT2M");

    /** Default of {@code aias.check.max-rows} (ADR-REG-019). */
    public static final int DEFAULT_MAX_ROWS = 100;

    /** Default of {@code aias.check.max-file-size} (ADR-REG-019). */
    public static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(10);

    /** Default of {@code aias.check.max-uploads} (ADR-DOC-016). */
    public static final int DEFAULT_MAX_UPLOADS = 20;

    /** Default of {@code aias.check.deadline-check-interval} (CHK CORE, REQ-CHK-080). */
    public static final Duration DEFAULT_DEADLINE_CHECK_INTERVAL = Duration.ofSeconds(30);

    /** Default of {@code aias.check.pipeline-threads} (CHK CORE, REQ-CHK-001). */
    public static final int DEFAULT_PIPELINE_THREADS = 4;

    public CheckLimitsProperties {
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        maxRows = maxRows == null ? DEFAULT_MAX_ROWS : maxRows;
        maxFileSize = maxFileSize == null ? DEFAULT_MAX_FILE_SIZE : maxFileSize;
        maxUploads = maxUploads == null ? DEFAULT_MAX_UPLOADS : maxUploads;
        deadlineCheckInterval = deadlineCheckInterval == null
                ? DEFAULT_DEADLINE_CHECK_INTERVAL : deadlineCheckInterval;
        pipelineThreads = pipelineThreads == null ? DEFAULT_PIPELINE_THREADS : pipelineThreads;
        // uploadWindow has no default: a missing value is refused by @NotNull at binding.
        // Bean Validation has no positivity constraint for Duration or DataSize; the same
        // "must be greater than 0" rule is applied here so an invalid limit fails start-up.
        requirePositive("aias.check.timeout", timeout);
        requirePositive("aias.check.deadline-check-interval", deadlineCheckInterval);
        if (uploadWindow != null) {
            requirePositive("aias.check.upload-window", uploadWindow);
        }
        if (maxFileSize.toBytes() <= 0) {
            throw new IllegalArgumentException(
                    "aias.check.max-file-size must be greater than 0: " + maxFileSize);
        }
    }

    private static void requirePositive(String key, Duration value) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(key + " must be greater than 0: " + value);
        }
    }
}
