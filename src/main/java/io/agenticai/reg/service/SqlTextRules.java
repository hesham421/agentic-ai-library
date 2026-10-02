package io.agenticai.reg.service;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The two text rules on a query's SQL (RULE-REG-006, RULE-REG-007; G3, G4, ADR-REG-008), checked
 * without a SQL parser — conservative, documented checks on the text:
 *
 * <ul>
 *   <li>RULE-REG-006 — the only parameter is the named bind parameter {@code :{inputName}}: any
 *       other {@code :name} or {@code :1} bind, a {@code ?} positional marker, or a
 *       {@code ${…}}, {@code #{…}} or <code>{{…}}</code> substitution marker is reported, the first in
 *       order of appearance.</li>
 *   <li>RULE-REG-007 — one SELECT statement, with or without a leading WITH clause: after trimming
 *       and stripping one trailing {@code ;}, the text starts with {@code SELECT} or {@code WITH}
 *       (case-insensitive, as a whole word), holds no further {@code ;}, and a {@code WITH} clause
 *       is followed by a {@code SELECT}. Anything else — {@code UPDATE}, {@code DELETE}, a second
 *       statement, a PL/SQL block, a parenthesised query — is refused.</li>
 * </ul>
 *
 * <p>Both checks look at the text outside single-quoted string literals, double-quoted
 * identifiers and {@code --} / {@code /* *}{@code /} comments: those are masked with spaces first,
 * so a {@code :} inside a literal is not a bind marker and a {@code ;} inside a comment is not a
 * second statement. The exact bind name is compared case-sensitively. Pure, no state.
 */
final class SqlTextRules {

    private static final Pattern MARKER = Pattern.compile(
            "\\$\\{[^}]*}|#\\{[^}]*}|\\{\\{[^}]*}}|\\?|:[A-Za-z_][A-Za-z0-9_]*|:[0-9]+");
    private static final Pattern STARTS_WITH_SELECT = Pattern.compile("^(?i)select(?![A-Za-z0-9_])");
    private static final Pattern STARTS_WITH_WITH = Pattern.compile("^(?i)with(?![A-Za-z0-9_])");
    private static final Pattern CONTAINS_SELECT = Pattern.compile("(?i)(?<![A-Za-z0-9_])select(?![A-Za-z0-9_])");

    private SqlTextRules() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * RULE-REG-006 — the first marker that is not the declared bind parameter, or empty.
     *
     * @param sqlText   the query text
     * @param inputName the declared input name; the only allowed marker is {@code ":" + inputName}
     */
    static Optional<String> firstUnboundMarker(String sqlText, String inputName) {
        Objects.requireNonNull(sqlText, "sqlText");
        Objects.requireNonNull(inputName, "inputName");
        String allowed = ":" + inputName;
        Matcher matcher = MARKER.matcher(mask(sqlText));
        while (matcher.find()) {
            String marker = matcher.group();
            if (!marker.equals(allowed)) {
                return Optional.of(marker);
            }
        }
        return Optional.empty();
    }

    /** RULE-REG-007 — whether the text is one SELECT statement (optionally starting with WITH). */
    static boolean isSingleSelect(String sqlText) {
        Objects.requireNonNull(sqlText, "sqlText");
        String masked = mask(sqlText).trim();
        if (masked.endsWith(";")) {
            masked = masked.substring(0, masked.length() - 1).trim();
        }
        if (masked.indexOf(';') >= 0) {
            return false;
        }
        if (STARTS_WITH_SELECT.matcher(masked).find()) {
            return true;
        }
        return STARTS_WITH_WITH.matcher(masked).find() && CONTAINS_SELECT.matcher(masked.substring(4)).find();
    }

    /**
     * The text with every string literal, quoted identifier and comment replaced by spaces of
     * the same length, so positions are preserved. An unterminated literal or comment masks to the
     * end of the text.
     */
    static String mask(String sqlText) {
        char[] out = sqlText.toCharArray();
        int n = out.length;
        int i = 0;
        while (i < n) {
            char c = out[i];
            if (c == '\'') {
                int j = i + 1;
                while (j < n) {
                    if (out[j] == '\'') {
                        if (j + 1 < n && out[j + 1] == '\'') {
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                blank(out, i, Math.min(n, j + 1));
                i = j + 1;
            } else if (c == '"') {
                int j = i + 1;
                while (j < n && out[j] != '"') {
                    j++;
                }
                blank(out, i, Math.min(n, j + 1));
                i = j + 1;
            } else if (c == '-' && i + 1 < n && out[i + 1] == '-') {
                int j = i;
                while (j < n && out[j] != '\n') {
                    j++;
                }
                blank(out, i, j);
                i = j;
            } else if (c == '/' && i + 1 < n && out[i + 1] == '*') {
                int j = i + 2;
                while (j + 1 < n && !(out[j] == '*' && out[j + 1] == '/')) {
                    j++;
                }
                int end = j + 1 < n ? j + 2 : n;
                blank(out, i, end);
                i = end;
            } else {
                i++;
            }
        }
        return new String(out);
    }

    private static void blank(char[] chars, int from, int to) {
        for (int k = from; k < to; k++) {
            if (chars[k] != '\n') {
                chars[k] = ' ';
            }
        }
    }
}
