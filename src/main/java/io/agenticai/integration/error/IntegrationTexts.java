package io.agenticai.integration.error;

/**
 * The public face of {@link IntegrationMessages} for INT code outside this package (the advice's
 * detail values, the SVC-API log lines such as the REQ-INT-039 WARN): the English texts live in
 * {@code messages.properties}, keyed by code, and are filled by the same mechanism (ADR-INT-017) —
 * this facade only widens its visibility, it decides nothing.
 */
public final class IntegrationTexts {

    private IntegrationTexts() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The English text of {@code key} with its named placeholders filled from {@code arguments},
     * in order of first appearance.
     *
     * @see IntegrationMessages#english(String, Object...)
     */
    public static String english(String key, Object... arguments) {
        return IntegrationMessages.english(key, arguments);
    }
}
