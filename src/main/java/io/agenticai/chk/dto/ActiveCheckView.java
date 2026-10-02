package io.agenticai.chk.dto;

import io.agenticai.chk.repository.ActiveCheckRow;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The API document's {@code ActiveCheckView} schema (API-CHK-001), field for field. A plain JSON
 * object — no envelope; never the key, the audit fields or any request data (REQ-CHK-082).
 * Documented here in Javadoc: springdoc is not on the classpath.
 *
 * @param checkId     DBF-CHK-002 — integer int64, required
 * @param checkStatus DBF-CHK-003 — the CHECK_STATUS code, {@code AWAITING_DOCUMENTS} or
 *                    {@code RUNNING}, at most 30 characters, required
 * @param deadlineAt  DBF-CHK-004 — ISO-8601 date-time, required: the end of the upload window
 *                    (AWAITING_DOCUMENTS) or of the Check timeout (RUNNING)
 */
public record ActiveCheckView(Long checkId, String checkStatus, OffsetDateTime deadlineAt) {

    /** The one mapping of the QR-CHK-001 projection to the response (A.3.8); no decision here. */
    public static ActiveCheckView of(ActiveCheckRow row) {
        Objects.requireNonNull(row, "row");
        return new ActiveCheckView(row.checkId(), row.checkStatus().storedValue(), row.deadlineAt());
    }
}
