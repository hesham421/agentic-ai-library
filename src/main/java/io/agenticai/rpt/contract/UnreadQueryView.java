package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * One service query of a completed report whose data could not be read (CON-RPT-003; API-RPT-001
 * schema {@code UnreadQueryView}). No query row is held.
 *
 * @param position  DBF-RPT-041 — order in the report, from 1
 * @param queryName DBF-RPT-042 — the service query name
 * @param detail    DBF-RPT-043 — why its data was not read
 */
public record UnreadQueryView(int position, String queryName, String detail) {

    public UnreadQueryView {
        Objects.requireNonNull(queryName, "queryName");
        Objects.requireNonNull(detail, "detail");
    }
}
