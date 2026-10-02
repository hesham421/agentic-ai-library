package io.agenticai.chk.contract;

import java.util.Objects;

/**
 * CON-CHK-008 — one document outcome of a completed Check, WITHOUT content (REQ-CHK-047).
 *
 * @param documentType the document type; {@code null} only for a host document whose type was NULL
 * @param sourceMode   the source mode code (CON-DOC-002)
 * @param readStatus   READ, MISSING or UNREADABLE (CON-DOC-001)
 * @param reason       the unreadable reason code (CON-DOC-001), UNREADABLE only
 * @param detail       the failure detail, UNREADABLE only — never content
 */
public record ReportDocumentOutcome(String documentType,
                                    String sourceMode,
                                    String readStatus,
                                    String reason,
                                    String detail) {

    public ReportDocumentOutcome {
        Objects.requireNonNull(sourceMode, "sourceMode");
        Objects.requireNonNull(readStatus, "readStatus");
    }
}
