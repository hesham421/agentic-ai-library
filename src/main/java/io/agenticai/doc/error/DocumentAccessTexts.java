package io.agenticai.doc.error;

/**
 * The public face of {@link DocumentAccessMessages} for the typed exceptions of the in-process
 * interface ({@code io.agenticai.doc.contract}, ADR-DOC-012) and for the RULE-DOC-005 notice:
 * their English texts live in {@code messages.properties} beside the catalog's, keyed by code, and
 * are filled by the same mechanism — this facade only widens its visibility, it decides nothing.
 */
public final class DocumentAccessTexts {

    private DocumentAccessTexts() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The English text of {@code key} with its named placeholders filled from {@code arguments},
     * in order of first appearance.
     *
     * @param key       an in-process rejection code of the SVC-API table, or the notice key
     * @param arguments the placeholder values
     * @see DocumentAccessMessages#english(String, Object...)
     */
    public static String english(String key, Object... arguments) {
        return DocumentAccessMessages.english(key, arguments);
    }
}
