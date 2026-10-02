package io.agenticai.reg.service;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A structure-free look at a service definition's YAML, taken <em>beside</em> the closed-structure
 * parse of {@code ServiceDefinitionParser}: the declared {@code service} and {@code version}
 * scalars, and the first query name declared twice. It composes the YAML node tree without
 * constructing objects (no tag instantiation) and with duplicate keys allowed, so it can answer
 * even for a definition the structured parser refuses:
 *
 * <ul>
 *   <li>the declared service code is what a REJECTED folder's Load Result row carries and what
 *       RULE-REG-002 counts — "every folder whose service definition declared a readable service
 *       code, whether or not that folder fails another rule";</li>
 *   <li>a query name declared twice is RULE-REG-019's case, which the structured parser can only
 *       report as a duplicate key.</li>
 * </ul>
 *
 * <p>A text that is not valid YAML, or not a mapping, yields an empty scan. Pure, no state.
 */
final class DefinitionScan {

    private static final int MAX_CODE_POINTS = 1_000_000;
    private static final int MAX_ALIASES = 50;
    private static final int MAX_NESTING_DEPTH = 10;

    static final DefinitionScan EMPTY = new DefinitionScan(null, null, null);

    private final String serviceCode;
    private final Integer versionNumber;
    private final String duplicateQueryName;

    private DefinitionScan(String serviceCode, Integer versionNumber, String duplicateQueryName) {
        this.serviceCode = serviceCode;
        this.versionNumber = versionNumber;
        this.duplicateQueryName = duplicateQueryName;
    }

    static DefinitionScan of(String yamlText) {
        if (yamlText == null) {
            return EMPTY;
        }
        Node root;
        try {
            root = newYaml().compose(new StringReader(yamlText));
        } catch (YAMLException e) {
            return EMPTY;
        }
        if (!(root instanceof MappingNode top)) {
            return EMPTY;
        }
        String service = null;
        Integer version = null;
        String duplicate = null;
        for (NodeTuple tuple : top.getValue()) {
            String key = scalar(tuple.getKeyNode());
            if (key == null) {
                continue;
            }
            switch (key) {
                case "service" -> {
                    if (service == null) {
                        service = scalar(tuple.getValueNode());
                    }
                }
                case "version" -> {
                    if (version == null) {
                        version = integer(scalar(tuple.getValueNode()));
                    }
                }
                case "queries" -> {
                    if (duplicate == null) {
                        duplicate = firstDuplicateKey(tuple.getValueNode());
                    }
                }
                default -> {
                    // not this scan's concern
                }
            }
        }
        return new DefinitionScan(service, version, duplicate);
    }

    /** The declared {@code service} text, as declared (not canonical), when present and not blank. */
    Optional<String> declaredServiceCode() {
        return serviceCode == null || serviceCode.isBlank() ? Optional.empty() : Optional.of(serviceCode);
    }

    /** The declared {@code version} when it is an integer. */
    Optional<Integer> declaredVersion() {
        return Optional.ofNullable(versionNumber);
    }

    /** The first query name that {@code queries} declares more than once. */
    Optional<String> firstDuplicateQueryName() {
        return Optional.ofNullable(duplicateQueryName);
    }

    private static String firstDuplicateKey(Node node) {
        if (!(node instanceof MappingNode mapping)) {
            return null;
        }
        Set<String> seen = new HashSet<>();
        List<NodeTuple> tuples = mapping.getValue();
        for (NodeTuple tuple : tuples) {
            String key = scalar(tuple.getKeyNode());
            if (key != null && !seen.add(key)) {
                return key;
            }
        }
        return null;
    }

    private static String scalar(Node node) {
        return node instanceof ScalarNode scalar ? scalar.getValue() : null;
    }

    private static Integer integer(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Yaml newYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(true);
        options.setAllowRecursiveKeys(false);
        options.setMaxAliasesForCollections(MAX_ALIASES);
        options.setCodePointLimit(MAX_CODE_POINTS);
        options.setNestingDepthLimit(MAX_NESTING_DEPTH);
        return new Yaml(new SafeConstructor(options));
    }
}
