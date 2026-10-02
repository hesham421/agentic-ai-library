package io.agenticai.doc.service;

import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;

import java.time.Instant;

/**
 * The two UNREADABLE outcomes the fetch procedure itself records, spelled in one place: the
 * Check's deadline passed before a step ran (OUT_OF_TIME, REQ-DOC-040) and a step failed with an
 * exception no adapter translated (READING_FAILED, REQ-DOC-037). Framework-free; a detail names
 * the failure and never document content.
 */
final class ReadFailures {

    private ReadFailures() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /** Whether the Check's deadline has passed — checked before every step (REQ-DOC-040). */
    static boolean passed(Instant deadline) {
        return Instant.now().isAfter(deadline);
    }

    /** OUT_OF_TIME — the deadline passed before the document was read. */
    static <T> Unreadable<T> outOfTime(Instant deadline) {
        return new Unreadable<>(UnreadableReason.OUT_OF_TIME,
                "the Check's deadline " + deadline + " passed before the document was read");
    }

    /** READING_FAILED — an unexpected failure of one document, named by its type and message. */
    static <T> Unreadable<T> readingFailed(RuntimeException failure) {
        String message = failure.getMessage();
        return new Unreadable<>(UnreadableReason.READING_FAILED,
                failure.getClass().getSimpleName()
                        + (message == null || message.isBlank() ? "" : ": " + message));
    }
}
