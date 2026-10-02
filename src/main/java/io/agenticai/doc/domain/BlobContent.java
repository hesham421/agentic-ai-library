package io.agenticai.doc.domain;

import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;

/**
 * The admission of a {@code blob} document's content column before any of it is streamed
 * (REQ-DOC-015, REQ-DOC-041, REQ-DOC-042; guardrail G8). The JDBC document source adapter
 * (PORTS-QUERY) measures the column with {@code Blob.length()}, asks this guard, and opens the
 * binary stream only on a {@link Read} — the guard runs before the stream exists, so an
 * oversized or empty column never has a byte read.
 *
 * <p>Framework-free: it decides on two numbers and touches no connection. The guard is the
 * {@code blob} counterpart of steps 4 and 5 of the host file adapter.
 */
public final class BlobContent {

    private BlobContent() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * Whether the content column may be streamed.
     *
     * @param length           the value of {@code Blob.length()}, or {@code null} when the column
     *                         is NULL
     * @param maxFileSizeBytes {@code aias.check.max-file-size} in bytes
     * @return {@link Read} of the length — the content is within the limit; stream exactly that
     *         many bytes. {@link Unreadable} NOT_FOUND when the column is NULL or empty
     *         (REQ-DOC-015); {@link Unreadable} TOO_LARGE, naming the length and the maximum,
     *         when it is larger than the maximum file size (REQ-DOC-042) — in both cases the
     *         content is not streamed
     */
    public static ReadOutcome<Long> admit(Long length, long maxFileSizeBytes) {
        if (length == null || length <= 0) {
            return new Unreadable<>(UnreadableReason.NOT_FOUND,
                    "the content column is " + (length == null ? "NULL" : "empty")
                            + "; the document has no content");
        }
        if (length > maxFileSizeBytes) {
            return new Unreadable<>(UnreadableReason.TOO_LARGE,
                    FileSize.tooLargeDetail("the content column", length, maxFileSizeBytes));
        }
        return new Read<>(length);
    }
}
