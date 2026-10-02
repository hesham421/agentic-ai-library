package io.agenticai.doc.domain;

import java.util.Objects;

/**
 * The outcome of one step of reading a document: either the value the step produced, or the
 * reason it could not be produced (CON-DOC-001). The one closed vocabulary every port, adapter
 * and domain service of the fetch pipeline speaks — the host file read ({@code byte[]}), the
 * BLOB admission, the format detection, the text and table extraction — so that an unreadable
 * item travels as a recorded outcome and is never dropped or thrown (REQ-DOC-036, REQ-DOC-037;
 * guardrail G6).
 *
 * <p>Framework-free, immutable. The service maps a {@link Read} to a READ Document Outcome and an
 * {@link Unreadable} to an UNREADABLE one carrying exactly its reason and detail.
 *
 * @param <T> what the step produces when it succeeds
 */
public sealed interface ReadOutcome<T> permits ReadOutcome.Read, ReadOutcome.Unreadable {

    /**
     * The step produced its value.
     *
     * @param value the produced value, never {@code null}. For a {@code byte[]} the array is the
     *              content itself, compared by identity, and is read, never written into
     */
    record Read<T>(T value) implements ReadOutcome<T> {

        public Read {
            Objects.requireNonNull(value, "value");
        }
    }

    /**
     * The step could not produce its value: exactly one {@link UnreadableReason} and a detail
     * text naming the failure (REQ-DOC-036, ADR-DOC-007). The detail names the failure — a
     * location, a size, an exception message — and never document content.
     */
    record Unreadable<T>(UnreadableReason reason, String detail) implements ReadOutcome<T> {

        public Unreadable {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
        }

        /**
         * The same outcome for a step that produces another type: the reason and the detail are
         * what matters, and they do not depend on {@code T}.
         */
        public <U> Unreadable<U> retyped() {
            return new Unreadable<>(reason, detail);
        }
    }

    /** Whether the step produced its value. */
    default boolean isRead() {
        return this instanceof Read<T>;
    }
}
