package io.agenticai.doc.port;

import io.agenticai.doc.domain.ReadOutcome;

/**
 * Outbound port of DOC to host file storage, the {@code path} fetch mode (REQ-DOC-005): one
 * document's bytes at the location the document source query returned. The adapter behind it is
 * the only class of DOC that opens the file system, and it only ever reads: this port has no
 * write, move, rename or delete method, and never will (REQ-DOC-049, REQ-DOC-050; guardrail
 * G3).
 *
 * <p>The port reports and decides nothing beyond containment and size: whatever cannot be read
 * comes back as a recorded {@link ReadOutcome.Unreadable} with its reason — NOT_FOUND,
 * OUTSIDE_STORAGE_ROOT, TOO_LARGE or READING_FAILED — and is never thrown (REQ-DOC-007,
 * REQ-DOC-009, REQ-DOC-011, REQ-DOC-042; guardrail G6).
 */
public interface HostFilePort {

    /**
     * The content of the file at {@code location}, resolved inside the configured storage root.
     *
     * @param location the path column's value of the document's row, as the host returned it —
     *                 relative to the storage root, or absolute; {@code null} or blank when the
     *                 row has no path
     * @return the bytes, or an UNREADABLE outcome; never throws
     */
    ReadOutcome<byte[]> read(String location);
}
