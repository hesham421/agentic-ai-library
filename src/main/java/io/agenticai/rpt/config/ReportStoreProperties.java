package io.agenticai.rpt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;

/**
 * The Report Store's retention configuration, bound from {@code aias.reports.*} (RPT CORE R1;
 * ADR-RPT-004, ADR-RPT-010). Environment configuration, never part of the deployable; the only
 * class binding this prefix (CU.6). The listing cap is not a property — it is the constant
 * {@link io.agenticai.rpt.domain.ReportLimits#LIST_LIMIT} (ADR-RPT-008).
 *
 * <p>The retention period is optional: absent, zero or negative is a valid start-up state in
 * which the purge deletes nothing and logs that it was skipped (REQ-RPT-044) — it never blocks
 * start-up, so the record is not {@code @Validated}. The purge schedule is checked at binding: a
 * value that is not a Spring cron expression fails start-up with the reason, rather than leaving
 * the purge unscheduled.
 *
 * @param retentionDays the report retention period in whole days (REQ-RPT-042, REQ-RPT-044);
 *                      {@code null} when not configured
 * @param purgeSchedule the cron expression of the purge (ADR-RPT-010); default
 *                      {@value #DEFAULT_PURGE_SCHEDULE}
 */
@ConfigurationProperties(prefix = "aias.reports")
public record ReportStoreProperties(
        Integer retentionDays,
        String purgeSchedule) {

    /** Default of {@code aias.reports.purge-schedule} — daily at 02:00 server time (ADR-RPT-010). */
    public static final String DEFAULT_PURGE_SCHEDULE = "0 0 2 * * *";

    public ReportStoreProperties {
        purgeSchedule = purgeSchedule == null || purgeSchedule.isBlank()
                ? DEFAULT_PURGE_SCHEDULE : purgeSchedule;
        if (!CronExpression.isValidExpression(purgeSchedule)) {
            throw new IllegalArgumentException(
                    "aias.reports.purge-schedule \"" + purgeSchedule + "\" is not a valid cron expression");
        }
    }

    /**
     * Whether a valid retention period is configured: a whole number of days greater than zero
     * (REQ-RPT-044, ADR-RPT-010). When {@code false} the purge deletes nothing.
     */
    public boolean hasValidRetentionPeriod() {
        return retentionDays != null && retentionDays > 0;
    }
}
