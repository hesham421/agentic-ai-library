package io.agenticai.chk.service;

import io.agenticai.chk.error.CheckEngineTexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Which service queries of a Check's version CHK runs (RULE-CHK-005, REQ-CHK-011, REQ-CHK-015):
 * the version's queries (read through CON-REG-003) minus the one whose name equals the version's
 * document source query name (read through CON-REG-002) — Document Access runs that one in every
 * fetch mode, so it is never sent by CHK. The comparison is exact (case-sensitive), as the query
 * names are business keys (CON-REG-003). The order of the version's queries is kept.
 *
 * <p>The skipped query is logged with RULE-CHK-005's message (key {@code CHK-RULE-005}) — an
 * internal log line only, never shown to the employee and never put in the report. Plain Java, no
 * Spring, no state.
 */
public final class ServiceQuerySelection {

    private static final Logger log = LoggerFactory.getLogger(ServiceQuerySelection.class);

    private ServiceQuerySelection() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The queries CHK sends.
     *
     * @param queries                 the version's queries, in their stored order
     * @param queryName               how to read a query's name
     * @param documentSourceQueryName the version's document source query name; {@code null} when it
     *                                names none (nothing is skipped)
     * @param <Q>                     the query type
     * @return the queries without the document source query, unmodifiable
     */
    public static <Q> List<Q> select(List<Q> queries, Function<Q, String> queryName, String documentSourceQueryName) {
        Objects.requireNonNull(queries, "queries");
        Objects.requireNonNull(queryName, "queryName");
        List<Q> selected = new ArrayList<>(queries.size());
        for (Q query : queries) {
            String name = queryName.apply(query);
            if (documentSourceQueryName != null && documentSourceQueryName.equals(name)) {
                log.info("{}", CheckEngineTexts.english("CHK-RULE-005", name));
                continue;
            }
            selected.add(query);
        }
        return List.copyOf(selected);
    }
}
