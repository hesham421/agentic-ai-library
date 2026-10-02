package io.agenticai.rpt.error;

/**
 * The public face of {@link ReportStoreMessages} for RPT's texts outside the HTTP catalog: the
 * typed refusals of the in-process interface (the SVC-API table, ADR-RPT-013 point 2) and the
 * documented log texts such as {@link #PURGE_SKIPPED} (REQ-RPT-044). Their English texts live in
 * {@code messages.properties} beside the catalog's, keyed by code, and are filled by the same
 * mechanism — this facade only widens its visibility, it decides nothing.
 */
public final class ReportStoreTexts {

    /**
     * Message key — NOT an error code — of the purge's skip log line, "Report purge skipped: no
     * valid report retention period is configured." (REQ-RPT-044, AC-RPT-052, ADR-RPT-010).
     */
    public static final String PURGE_SKIPPED = "RPT-LOG-PURGE-SKIPPED";

    private ReportStoreTexts() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The English text of {@code key} with its named placeholders filled from {@code arguments},
     * in order of first appearance.
     *
     * @param key       an in-process refusal code of the SVC-API table, or a documented message key
     * @param arguments the placeholder values
     * @see ReportStoreMessages#english(String, Object...)
     */
    public static String english(String key, Object... arguments) {
        return ReportStoreMessages.english(key, arguments);
    }
}
