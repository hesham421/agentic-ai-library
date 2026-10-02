package io.agenticai.reg.domain;

import java.util.Objects;

/**
 * The outcome of {@link ServiceDefinitionParser#parse}. A parse that does not yield a
 * definition is a REJECTED load reason, never an HTTP error (ADR-REG-011), so it is a value
 * the load run records — not an exception.
 */
public sealed interface ServiceDefinitionParseResult {

    /** The YAML held the closed structure: here is the definition as declared. */
    record Parsed(ParsedServiceDefinition definition) implements ServiceDefinitionParseResult {

        public Parsed {
            Objects.requireNonNull(definition, "definition");
        }
    }

    /**
     * The YAML declares an element the structure does not define (RULE-REG-012).
     *
     * @param element the first offending element, as a dotted path from the top level —
     *                {@code timeout}, {@code documents.storage_root},
     *                {@code queries.<name>.max_rows}
     */
    record ElementNotAllowed(String element) implements ServiceDefinitionParseResult {

        public ElementNotAllowed {
            Objects.requireNonNull(element, "element");
        }
    }

    /**
     * The text is not a service definition at all: it is not valid YAML, it is not a mapping, a
     * key is declared twice, or a declared value has the wrong type ({@code version: abc}).
     *
     * @param detail what was found, for the load report; never thrown
     */
    record Malformed(String detail) implements ServiceDefinitionParseResult {

        public Malformed {
            Objects.requireNonNull(detail, "detail");
        }
    }
}
