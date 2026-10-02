package io.agenticai.reg.domain;

import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedApproval;
import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedDocuments;
import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedQuery;
import io.agenticai.reg.domain.ServiceDefinitionParseResult.ElementNotAllowed;
import io.agenticai.reg.domain.ServiceDefinitionParseResult.Malformed;
import io.agenticai.reg.domain.ServiceDefinitionParseResult.Parsed;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses a service definition's YAML text into the closed structure of RULE-REG-012:
 * {@code service, version, input, queries, documents, approval}, each with the elements the
 * SRS names and no other. Framework-free, no I/O, no state: the text is passed in and a
 * {@link ServiceDefinitionParseResult} comes back.
 *
 * <p>What it decides: whether the text is YAML of the right shape, and whether every element
 * at every level is one the structure defines. The first element it does not define is
 * reported by its dotted path ({@link ElementNotAllowed}); a text that is not a mapping, a
 * duplicate key or a value of the wrong type is {@link Malformed}. It decides no business
 * rule: an absent section, an unknown fetch mode, a duplicate document type or an over-length
 * value is passed through as declared for the load run to judge.
 *
 * <p>The text is untrusted data: it is read with SnakeYAML's {@link SafeConstructor} (no object
 * instantiation), duplicate keys refused, aliases and size bounded.
 *
 * <p>Two element names are this class's choice, because no analysis artifact spells them:
 * the connection of a query ({@link #QUERY_CONNECTION_KEY}) and the document source query
 * ({@link #DOCUMENTS_SOURCE_KEY}). Each lives in its one constant.
 */
public final class ServiceDefinitionParser {

    /** Top-level elements of the closed structure (RULE-REG-012). */
    public static final String SERVICE_KEY = "service";
    public static final String VERSION_KEY = "version";
    public static final String INPUT_KEY = "input";
    public static final String QUERIES_KEY = "queries";
    public static final String DOCUMENTS_KEY = "documents";
    public static final String APPROVAL_KEY = "approval";

    /** Elements of one query. */
    public static final String QUERY_SQL_KEY = "sql";
    /** The connection name of a query — a name no artifact states; held here only. */
    public static final String QUERY_CONNECTION_KEY = "connection";

    /** Elements of {@code documents}. */
    public static final String DOCUMENTS_FETCH_KEY = "fetch";
    /** The document source query of {@code documents} — a name no artifact states; held here only. */
    public static final String DOCUMENTS_SOURCE_KEY = "source";
    public static final String DOCUMENTS_TYPE_COLUMN_KEY = "type_column";
    public static final String DOCUMENTS_PATH_COLUMN_KEY = "path_column";
    public static final String DOCUMENTS_CONTENT_COLUMN_KEY = "content_column";
    public static final String DOCUMENTS_REQUIRED_KEY = "required";

    /** Elements of {@code approval}. */
    public static final String APPROVAL_ENABLED_KEY = "enabled";
    public static final String APPROVAL_API_KEY = "api";

    private static final Set<String> TOP_LEVEL_KEYS = Set.of(
            SERVICE_KEY, VERSION_KEY, INPUT_KEY, QUERIES_KEY, DOCUMENTS_KEY, APPROVAL_KEY);
    private static final Set<String> QUERY_KEYS = Set.of(QUERY_SQL_KEY, QUERY_CONNECTION_KEY);
    private static final Set<String> DOCUMENTS_KEYS = Set.of(
            DOCUMENTS_FETCH_KEY, DOCUMENTS_SOURCE_KEY, DOCUMENTS_TYPE_COLUMN_KEY,
            DOCUMENTS_PATH_COLUMN_KEY, DOCUMENTS_CONTENT_COLUMN_KEY, DOCUMENTS_REQUIRED_KEY);
    private static final Set<String> APPROVAL_KEYS = Set.of(APPROVAL_ENABLED_KEY, APPROVAL_API_KEY);

    /** Bounds on the untrusted text: a service definition is a few kilobytes, never megabytes. */
    private static final int MAX_CODE_POINTS = 1_000_000;
    private static final int MAX_ALIASES = 50;
    private static final int MAX_NESTING_DEPTH = 10;

    /**
     * Parses {@code yamlText}.
     *
     * @param yamlText the service definition file's text — never {@code null} (API misuse)
     * @return the parsed definition, the first element the structure does not define, or why
     *         the text is not a service definition
     */
    public ServiceDefinitionParseResult parse(String yamlText) {
        Objects.requireNonNull(yamlText, "yamlText");
        Object root;
        try {
            root = newYaml().load(yamlText);
        } catch (YAMLException e) {
            return new Malformed("the service definition is not valid YAML: " + e.getMessage());
        }
        if (root == null) {
            return new Malformed("the service definition is empty");
        }
        if (!(root instanceof Map<?, ?> top)) {
            return new Malformed("the service definition is not a mapping of elements");
        }
        return readDefinition(top);
    }

    private static Yaml newYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setAllowRecursiveKeys(false);
        options.setMaxAliasesForCollections(MAX_ALIASES);
        options.setCodePointLimit(MAX_CODE_POINTS);
        options.setNestingDepthLimit(MAX_NESTING_DEPTH);
        return new Yaml(new SafeConstructor(options));
    }

    private static ServiceDefinitionParseResult readDefinition(Map<?, ?> top) {
        String unknown = firstUnknownKey(top, TOP_LEVEL_KEYS, "");
        if (unknown != null) {
            return new ElementNotAllowed(unknown);
        }

        Object service = top.get(SERVICE_KEY);
        if (service != null && !(service instanceof String)) {
            return new Malformed(notText(SERVICE_KEY));
        }
        Object version = top.get(VERSION_KEY);
        if (version != null && !(version instanceof Integer)) {
            return new Malformed("'" + VERSION_KEY + "' is not an integer: " + describe(version));
        }
        Object input = top.get(INPUT_KEY);
        if (input != null && !(input instanceof String)) {
            return new Malformed(notText(INPUT_KEY));
        }

        List<ParsedQuery> queries = new ArrayList<>();
        Object queriesNode = top.get(QUERIES_KEY);
        if (queriesNode != null) {
            if (!(queriesNode instanceof Map<?, ?> queriesMap)) {
                return new Malformed("'" + QUERIES_KEY + "' is not a mapping of query names");
            }
            for (Map.Entry<?, ?> entry : queriesMap.entrySet()) {
                String queryName = String.valueOf(entry.getKey());
                String path = QUERIES_KEY + "." + queryName;
                if (!(entry.getValue() instanceof Map<?, ?> queryMap)) {
                    return new Malformed("'" + path + "' is not a mapping");
                }
                unknown = firstUnknownKey(queryMap, QUERY_KEYS, path + ".");
                if (unknown != null) {
                    return new ElementNotAllowed(unknown);
                }
                Object sql = queryMap.get(QUERY_SQL_KEY);
                if (sql != null && !(sql instanceof String)) {
                    return new Malformed(notText(path + "." + QUERY_SQL_KEY));
                }
                Object connection = queryMap.get(QUERY_CONNECTION_KEY);
                if (connection != null && !(connection instanceof String)) {
                    return new Malformed(notText(path + "." + QUERY_CONNECTION_KEY));
                }
                queries.add(new ParsedQuery(queryName, (String) connection, (String) sql));
            }
        }

        ParsedDocuments documents = null;
        Object documentsNode = top.get(DOCUMENTS_KEY);
        if (documentsNode != null) {
            if (!(documentsNode instanceof Map<?, ?> documentsMap)) {
                return new Malformed("'" + DOCUMENTS_KEY + "' is not a mapping");
            }
            unknown = firstUnknownKey(documentsMap, DOCUMENTS_KEYS, DOCUMENTS_KEY + ".");
            if (unknown != null) {
                return new ElementNotAllowed(unknown);
            }
            String[] texts = new String[5];
            String[] textKeys = {DOCUMENTS_FETCH_KEY, DOCUMENTS_SOURCE_KEY, DOCUMENTS_TYPE_COLUMN_KEY,
                    DOCUMENTS_PATH_COLUMN_KEY, DOCUMENTS_CONTENT_COLUMN_KEY};
            for (int i = 0; i < textKeys.length; i++) {
                Object value = documentsMap.get(textKeys[i]);
                if (value != null && !(value instanceof String)) {
                    return new Malformed(notText(DOCUMENTS_KEY + "." + textKeys[i]));
                }
                texts[i] = (String) value;
            }
            List<String> required = new ArrayList<>();
            Object requiredNode = documentsMap.get(DOCUMENTS_REQUIRED_KEY);
            if (requiredNode != null) {
                if (!(requiredNode instanceof List<?> requiredList)) {
                    return new Malformed("'" + DOCUMENTS_KEY + "." + DOCUMENTS_REQUIRED_KEY
                            + "' is not a list");
                }
                for (Object item : requiredList) {
                    if (!(item instanceof String type)) {
                        return new Malformed("'" + DOCUMENTS_KEY + "." + DOCUMENTS_REQUIRED_KEY
                                + "' holds an item that is not text: " + describe(item));
                    }
                    required.add(type);
                }
            }
            documents = new ParsedDocuments(texts[0], texts[1], texts[2], texts[3], texts[4], required);
        }

        ParsedApproval approval = null;
        Object approvalNode = top.get(APPROVAL_KEY);
        if (approvalNode != null) {
            if (!(approvalNode instanceof Map<?, ?> approvalMap)) {
                return new Malformed("'" + APPROVAL_KEY + "' is not a mapping");
            }
            unknown = firstUnknownKey(approvalMap, APPROVAL_KEYS, APPROVAL_KEY + ".");
            if (unknown != null) {
                return new ElementNotAllowed(unknown);
            }
            Object enabled = approvalMap.get(APPROVAL_ENABLED_KEY);
            if (enabled != null && !(enabled instanceof Boolean)) {
                return new Malformed("'" + APPROVAL_KEY + "." + APPROVAL_ENABLED_KEY
                        + "' is not true or false: " + describe(enabled));
            }
            Object api = approvalMap.get(APPROVAL_API_KEY);
            if (api != null && !(api instanceof String)) {
                return new Malformed(notText(APPROVAL_KEY + "." + APPROVAL_API_KEY));
            }
            approval = new ParsedApproval((Boolean) enabled, (String) api);
        }

        return new Parsed(new ParsedServiceDefinition(
                (String) service, (Integer) version, (String) input, queries, documents, approval));
    }

    /** The dotted path of the first key of {@code map} outside {@code allowed}, or {@code null}. */
    private static String firstUnknownKey(Map<?, ?> map, Set<String> allowed, String prefix) {
        for (Object key : map.keySet()) {
            if (!(key instanceof String name) || !allowed.contains(name)) {
                return prefix + key;
            }
        }
        return null;
    }

    private static String notText(String path) {
        return "'" + path + "' is not text";
    }

    /** The value's kind for the report — never a secret-bearing dump of the whole document. */
    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return value.getClass().getSimpleName();
    }
}
