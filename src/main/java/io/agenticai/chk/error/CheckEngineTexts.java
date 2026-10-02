package io.agenticai.chk.error;

/**
 * The public face of {@link CheckEngineMessages} for the typed exceptions of the in-process
 * interface (the start and upload-confirmation rejections, ADR-CHK-018): their English texts live
 * in {@code messages.properties} beside the catalog's, keyed by code, and are filled by the same
 * mechanism — this facade only widens its visibility, it decides nothing.
 */
public final class CheckEngineTexts {

    private CheckEngineTexts() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The English text of {@code key} with its named placeholders filled from {@code arguments},
     * in order of first appearance.
     *
     * @param key       an error code whose text is in {@code messages.properties}
     * @param arguments the placeholder values
     * @see CheckEngineMessages#english(String, Object...)
     */
    public static String english(String key, Object... arguments) {
        return CheckEngineMessages.english(key, arguments);
    }
}
