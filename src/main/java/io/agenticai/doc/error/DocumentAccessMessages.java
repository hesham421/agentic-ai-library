package io.agenticai.doc.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The English message texts of the DOC error catalog, read from {@code messages.properties}
 * keyed by error code. Framework-free so that domain classes can raise a
 * {@link DocumentAccessException} without Spring.
 *
 * <p>Templates carry the catalog's named placeholders (for example {@code {checkId}}); the
 * arguments given with a code fill the distinct placeholders in order of first appearance.
 * Arabic texts are {@code PENDING ADR-DOC-012} and are never invented here.
 */
final class DocumentAccessMessages {

    private static final String BUNDLE = "messages";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");
    private static final ResourceBundle ENGLISH = ResourceBundle.getBundle(
            BUNDLE,
            Locale.ENGLISH,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

    private DocumentAccessMessages() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The catalog's English text of {@code code} with its placeholders filled from
     * {@code arguments}.
     *
     * @throws java.util.MissingResourceException when {@code code} has no catalog text (API misuse)
     * @throws IllegalArgumentException           when fewer arguments than placeholders are given
     */
    static String english(String code, Object... arguments) {
        Objects.requireNonNull(code, "code");
        String template = ENGLISH.getString(code);
        Object[] args = arguments == null ? new Object[0] : arguments;

        List<String> names = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder filled = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            int index = names.indexOf(name);
            if (index < 0) {
                names.add(name);
                index = names.size() - 1;
            }
            if (index >= args.length) {
                throw new IllegalArgumentException(
                        "Message of " + code + " needs an argument for {" + name + "}");
            }
            matcher.appendReplacement(filled, Matcher.quoteReplacement(String.valueOf(args[index])));
        }
        matcher.appendTail(filled);
        return filled.toString();
    }
}
