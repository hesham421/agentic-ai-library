package io.agenticai.doc.contract;

/**
 * CON-DOC-005 — what the end of a Check answers: {@code {deletedCount}}, every Uploaded Document
 * row the call deleted — the Check's own and those swept of earlier ended Checks (REQ-DOC-054,
 * REQ-DOC-062). A Check with no Uploaded Document answers 0; repeating the call answers 0.
 *
 * @param deletedCount the rows deleted by the call
 */
public record EndCheckResult(int deletedCount) {

    public EndCheckResult {
        if (deletedCount < 0) {
            throw new IllegalArgumentException("deletedCount cannot be negative: " + deletedCount);
        }
    }
}
