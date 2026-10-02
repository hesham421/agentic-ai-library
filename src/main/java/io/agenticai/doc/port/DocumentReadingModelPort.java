package io.agenticai.doc.port;

import io.agenticai.doc.domain.ReadOutcome;

import java.time.Instant;

/**
 * The document-reading step (REQ-DOC-027): one document in, its content as text out. Scanned
 * PDFs and images are read by the document-reading model — an OCR engine or a vision model
 * reached through the platform's provider-neutral model interface (ADR-DOC-004, REQ-DOC-032) —
 * and this port is the only way DOC reaches it. The adapter behind it owns the model's
 * configuration ({@code aias.documents.reading-model.*}, REQ-DOC-030), the free-tier gate
 * (ADR-DOC-009) and the translation of every failure into a recorded outcome.
 *
 * <p>Each call stands alone: one document, no content of an earlier call (REQ-DOC-057). The
 * text returned is the model's output as data — it is never interpreted as a command
 * (REQ-DOC-045, REQ-DOC-047).
 */
public interface DocumentReadingModelPort {

    /**
     * Reads one document with the document-reading model.
     *
     * @param content   the document's bytes, exactly as fetched
     * @param mediaType the document's media type as the format detector read it from the
     *                  content signature (e.g. {@code application/pdf}, {@code image/png})
     * @param deadline  the Check's deadline (REQ-DOC-040): the call is bounded by the time
     *                  remaining until it, and a document not read in time is UNREADABLE /
     *                  OUT_OF_TIME
     * @return the text the model returned, or an UNREADABLE outcome — MODEL_NOT_PERMITTED when
     *         a tier FREE model is configured in a data class REAL environment (REQ-DOC-059),
     *         READING_FAILED when no model is configured (REQ-DOC-033) or the call failed
     *         (REQ-DOC-029), OUT_OF_TIME when the deadline was reached (REQ-DOC-040); nothing
     *         is thrown for an external failure
     */
    ReadOutcome<String> read(byte[] content, String mediaType, Instant deadline);
}
