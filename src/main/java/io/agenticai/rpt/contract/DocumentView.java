package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * One document outcome of a completed report, without content (CON-RPT-003, REQ-RPT-019;
 * API-RPT-001 schema {@code DocumentView}).
 *
 * @param position         DBF-RPT-031 — order in the report, from 1
 * @param documentType     DBF-RPT-032 — the document type
 * @param sourceMode       DBF-RPT-033 — FETCH_MODE code: path, blob or manual
 * @param readStatus       DBF-RPT-034 — DOCUMENT_READ_STATUS code: READ, MISSING or UNREADABLE
 * @param unreadableReason DBF-RPT-035 — UNREADABLE_REASON code, UNREADABLE only, else {@code null}
 * @param detail           DBF-RPT-036 — the detail text, or {@code null}
 */
public record DocumentView(int position,
                           String documentType,
                           String sourceMode,
                           String readStatus,
                           String unreadableReason,
                           String detail) {

    public DocumentView {
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(sourceMode, "sourceMode");
        Objects.requireNonNull(readStatus, "readStatus");
    }
}
