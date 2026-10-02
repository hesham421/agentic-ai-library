package io.agenticai.reg.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The English texts of the load reason codes, read from {@code messages.properties} keyed by
 * {@code REG-LOAD-*} code — the same bundle and the same placeholder convention as the error
 * catalog's {@code ServiceRegistryMessages} (which is package-private to {@code reg.error} and
 * frozen, so its few lines of formatting are repeated here rather than widened).
 *
 * <p>Templates carry named placeholders ({@code {folder}}, {@code {serviceCode}}, …); the
 * arguments of a {@link LoadReason} fill the distinct placeholders in order of first appearance.
 * A {@code null} argument renders as an empty string (a value that was not declared). Arabic
 * texts are {@code PENDING ADR-REG-011} and are never invented here.
 */
final class LoadReasonMessages {

    private static final String BUNDLE = "messages";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");
    private static final ResourceBundle ENGLISH = ResourceBundle.getBundle(
            BUNDLE,
            Locale.ENGLISH,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

    private LoadReasonMessages() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /** The English text of {@code reason}'s code with its placeholders filled. */
    static String english(LoadReason reason) {
        Objects.requireNonNull(reason, "reason");
        return english(reason.code(), reason.arguments());
    }

    /**
     * @throws java.util.MissingResourceException when {@code code} has no text (API misuse)
     * @throws IllegalArgumentException           when fewer arguments than placeholders are given
     */
    static String english(String code, List<Object> arguments) {
        Objects.requireNonNull(code, "code");
        String template = ENGLISH.getString(code);
        List<Object> args = arguments == null ? List.of() : arguments;

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
            if (index >= args.size()) {
                throw new IllegalArgumentException(
                        "Message of " + code + " needs an argument for {" + name + "}");
            }
            Object value = args.get(index);
            matcher.appendReplacement(filled, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(filled);
        return filled.toString();
    }
}
