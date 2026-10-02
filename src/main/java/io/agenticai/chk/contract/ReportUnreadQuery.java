package io.agenticai.chk.contract;

import java.util.Objects;

/**
 * CON-CHK-008 — a service query whose data was not read (ADR-CHK-014): its name and a detail text.
 *
 * @param queryName the query's name
 * @param detail    why it was not read — never the SQL text, a credential or row data
 */
public record ReportUnreadQuery(String queryName, String detail) {

    public ReportUnreadQuery {
        Objects.requireNonNull(queryName, "queryName");
        Objects.requireNonNull(detail, "detail");
    }
}
