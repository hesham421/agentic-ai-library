package io.agenticai.chk.adapter;

import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.QueryResult.NotRead;
import io.agenticai.chk.port.ServiceQuery;
import io.agenticai.reg.contract.ConnectionSettings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The guards {@link McpServiceQueryAdapter} runs <strong>before</strong> a service query is sent
 * (RULE-CHK-002, RULE-CHK-003). Each refusal is a recorded {@link NotRead} carrying the rule's
 * message (keys {@code CHK-RULE-002} / {@code CHK-RULE-003} in {@code messages.properties}) as
 * detail, never an exception (REQ-CHK-012, REQ-CHK-014). Pure, no state.
 *
 * <p>RULE-CHK-002 reads "not of type {@code mcp} or not declared read-only". CON-REG-011 carries no
 * read-only flag; CON-REG-005 / RULE-REG-015 guarantee that every connection REG supplies is
 * declared read-only (the same reasoning as DOC's RULE-DOC-007 guard, recorded as an OPEN gap). So
 * the guard refuses a connection REG could not supply (absent settings — nothing declares it
 * read-only) and a supplied connection whose type is not {@code mcp}.
 *
 * <p>RULE-CHK-003 refuses a query whose text has no {@code :{inputName}} token: carrying the
 * request number would then mean changing the text, which is never done. The token is matched
 * boundary-aware — not preceded by an identifier character or a {@code :} (so {@code ::cast} or
 * {@code :requestIdX} do not count), not followed by an identifier character — and only outside
 * string literals, quoted identifiers and comments (masked as REG's RULE-REG-006 check masks them).
 * The text itself is never changed.
 */
final class ServiceQueryGuards {

    /** The stored value of the {@code mcp} connection type (CON-REG-005, closed lookup CONNECTION_TYPE). */
    static final String MCP_CONNECTION_TYPE = "mcp";

    static final String RULE_CHK_002 = "CHK-RULE-002";
    static final String RULE_CHK_003 = "CHK-RULE-003";

    private ServiceQueryGuards() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * RULE-CHK-002: the refusal when the connection is absent or not of type {@code mcp};
     * {@code null} when it may be used.
     */
    static NotRead refuseUnlessReadOnlyMcp(ServiceQuery query, ConnectionSettings connection) {
        if (connection != null && MCP_CONNECTION_TYPE.equals(connection.connectionType())) {
            return null;
        }
        return new NotRead(CheckEngineTexts.english(RULE_CHK_002, query.queryName(), query.connectionName()));
    }

    /**
     * RULE-CHK-003: the refusal when the SQL text has no bind token {@code :{inputName}};
     * {@code null} when it has one.
     */
    static NotRead refuseUnlessBindParameter(ServiceQuery query) {
        if (hasBindToken(query.sqlText(), query.inputName())) {
            return null;
        }
        return new NotRead(CheckEngineTexts.english(RULE_CHK_003, query.queryName(), query.inputName()));
    }

    /** Whether {@code sqlText} holds the token {@code :{inputName}}, boundary-aware, outside literals and comments. */
    static boolean hasBindToken(String sqlText, String inputName) {
        Objects.requireNonNull(sqlText, "sqlText");
        Objects.requireNonNull(inputName, "inputName");
        if (inputName.isEmpty()) {
            return false;
        }
        Pattern token = Pattern.compile("(?<![A-Za-z0-9_:]):" + Pattern.quote(inputName) + "(?![A-Za-z0-9_])");
        return token.matcher(mask(sqlText)).find();
    }

    /**
     * The failure text of a channel exception — its message, as the host gave it (REQ-CHK-049:
     * "the failure text"; AC-CHK-051), or its type when it has none. When the message would carry
     * the SQL text, only the type is given (E.4.3).
     */
    static String failureText(Throwable e, String sqlText) {
        String message = e.getMessage();
        if (message == null || message.isBlank() || (!sqlText.isBlank() && message.contains(sqlText.strip()))) {
            return e.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * The text with every single-quoted literal, double-quoted identifier and {@code --} /
     * {@code /* *}{@code /} comment replaced by spaces of the same length. An unterminated literal or
     * comment masks to the end of the text.
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
