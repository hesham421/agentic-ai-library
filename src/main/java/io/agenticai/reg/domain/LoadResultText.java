package io.agenticai.reg.domain;

/**
 * Fits Load Result text to its column (RULE-REG-028, REQ-REG-074, ADR-REG-022): the subject
 * name, service code and reason of a Load Result are shortened to {@link #SUBJECT_NAME_LIMIT},
 * {@link #SERVICE_CODE_LIMIT} and {@link #REASON_LIMIT} characters, the last kept character
 * replaced by {@code "…"} when shortened — so recording an outcome can never raise a length error.
 * This is the only truncation in REG, and it touches report text only, never configuration.
 *
 * <p>Applied by the single Load Result writer (the load run's recorder, a service) before every
 * insert; nothing here decides anything. Lengths are counted in Unicode code points, as the
 * columns are {@code VARCHAR2(n CHAR)}.
 */
public final class LoadResultText {

    /** Characters kept in {@code REG_LOAD_RESULT.SUBJECT_NAME} ({@code VARCHAR2(200 CHAR)}, DBF-REG-043). */
    public static final int SUBJECT_NAME_LIMIT = 200;

    /** Characters kept in {@code REG_LOAD_RESULT.SERVICE_CODE} ({@code VARCHAR2(100 CHAR)}, DBF-REG-044). */
    public static final int SERVICE_CODE_LIMIT = 100;

    /** Characters kept in {@code REG_LOAD_RESULT.REASON} ({@code VARCHAR2(1000 CHAR)}, DBF-REG-047). */
    public static final int REASON_LIMIT = 1000;

    /** The character that ends a shortened value (U+2026 HORIZONTAL ELLIPSIS). */
    private static final String ELLIPSIS = "…";

    private LoadResultText() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * {@code value} unchanged when it is {@code null} or holds at most {@code limit} characters;
     * otherwise its first {@code limit - 1} characters followed by {@code "…"}, so the result
     * holds exactly {@code limit} characters. Characters are Unicode code points: a supplementary
     * character counts as one and is never split.
     *
     * @param value the text to fit, or {@code null}
     * @param limit the column's declared length in characters, at least 1
     * @throws IllegalArgumentException when {@code limit} is below 1 — a misuse, not a rule
     */
    public static String fit(String value, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1: " + limit);
        }
        if (value == null) {
            return null;
        }
        int length = value.length();
        if (length <= limit) {
            return value;
        }
        int codePoints = value.codePointCount(0, length);
        if (codePoints <= limit) {
            return value;
        }
        int keptEnd = value.offsetByCodePoints(0, limit - 1);
        return value.substring(0, keptEnd) + ELLIPSIS;
    }
}
