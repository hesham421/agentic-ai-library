package io.agenticai.chk.port;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The outcome of one service query (REQ-CHK-049, REQ-CHK-050; ADR-CHK-014): either its rows, or
 * "not read" with a detail text. A failure is always an outcome, never an exception, so the Check
 * continues and nothing is skipped silently (G6).
 */
public sealed interface QueryResult permits QueryResult.Rows, QueryResult.NotRead {

    /**
     * The query was read in full: at most {@code maxRows} rows, each mapping a column name, as the
     * host names it, to its value ({@code null} for an SQL NULL), in the host's order. Unmodifiable.
     */
    record Rows(List<Map<String, Object>> rows) implements QueryResult {

        public Rows {
            rows = Objects.requireNonNull(rows, "rows").stream()
                    .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(row, "row"))))
                    .toList();
        }
    }

    /**
     * The query's data was not read: refused by a guard (RULE-CHK-002, RULE-CHK-003), failed
     * (REQ-CHK-049), over the row limit (REQ-CHK-050) or out of time. The detail names why — never
     * the SQL text, a credential or row data. No row is kept.
     */
    record NotRead(String detail) implements QueryResult {

        public NotRead {
            Objects.requireNonNull(detail, "detail");
        }

        /** The report entry of this query (ADR-CHK-014). */
        public UnreadQuery toUnread(String queryName) {
            return new UnreadQuery(queryName, detail);
        }
    }
}
