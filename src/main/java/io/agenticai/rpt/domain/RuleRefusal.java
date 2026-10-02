package io.agenticai.rpt.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A refusal decided by an RPT domain class: the {@link RefusalReason} and the values of its
 * message placeholders, by name, in order of appearance ({@code {checkId}} excluded — the caller
 * knows it). An immutable value; plain Java, no framework. SVC-API maps it to its in-process
 * rejection table (ADR-RPT-012, ADR-RPT-013).
 *
 * @param reason       why the operation is refused
 * @param placeholders the placeholder values by name — exactly {@link RefusalReason#placeholders()}
 */
public record RuleRefusal(RefusalReason reason, Map<String, String> placeholders) {

    public RuleRefusal {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(placeholders, "placeholders");
        if (!List.copyOf(placeholders.keySet()).equals(reason.placeholders())) {
            throw new IllegalArgumentException(
                    reason + " needs the placeholders " + reason.placeholders()
                            + ", got " + placeholders.keySet());
        }
        placeholders = Collections.unmodifiableMap(new LinkedHashMap<>(placeholders));
    }

    /**
     * A refusal whose placeholder values are given in the order of
     * {@link RefusalReason#placeholders()}.
     *
     * @throws IllegalArgumentException when the number of values does not match
     */
    public static RuleRefusal of(RefusalReason reason, Object... values) {
        Objects.requireNonNull(reason, "reason");
        List<String> names = reason.placeholders();
        Object[] given = values == null ? new Object[0] : values;
        if (given.length != names.size()) {
            throw new IllegalArgumentException(
                    reason + " needs " + names.size() + " placeholder values, got " + given.length);
        }
        Map<String, String> filled = new LinkedHashMap<>();
        for (int i = 0; i < given.length; i++) {
            filled.put(names.get(i), String.valueOf(given[i]));
        }
        return new RuleRefusal(reason, filled);
    }

    /** The placeholder values in order of appearance, ready for the message text. */
    public Object[] arguments() {
        return placeholders.values().toArray();
    }
}
