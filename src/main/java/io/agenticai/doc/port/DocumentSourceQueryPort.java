package io.agenticai.doc.port;

import io.agenticai.doc.domain.FetchMode;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.reg.contract.ConnectionSettings;

import java.time.Instant;
import java.util.List;

/**
 * Outbound port of DOC to the host's document source (ADR-DOC-001): runs the version's document
 * source query and returns its rows. DOC runs that query itself, in both query modes — through
 * the platform MCP query channel for {@code path} (REQ-DOC-004), over the read-only {@code jdbc}
 * connection for {@code blob} (REQ-DOC-012) — and each mode has its own adapter behind this one
 * port; {@link #mode()} says which, so the fetch procedure selects the adapter by the version's
 * fetch mode without naming it.
 *
 * <p>Contract of every adapter:
 * <ul>
 *   <li>the SQL text is sent <strong>exactly as stored</strong> — no statement of the adapter's
 *       own, no text built from request data (REQ-DOC-051; G4);</li>
 *   <li>the request number is passed as ONE bound value under the parameter
 *       {@link DocumentSourceQuery#bindParameter()}, never concatenated (REQ-DOC-004,
 *       REQ-DOC-012; AIAS-5);</li>
 *   <li>the limit {@link DocumentSourceQuery#rowLimit()} ({@code maxRows + 1}) is requested so
 *       that an over-limit result is detected and never truncated silently (REQ-DOC-039);</li>
 *   <li>the guards run before any query: a connection REG could not supply (RULE-DOC-007 —
 *       REG declares every connection it supplies read-only, RULE-REG-015), and for {@code blob}
 *       a connection not of type {@code jdbc} (RULE-DOC-006) — in both cases nothing is sent;</li>
 *   <li>a refused guard, a query error, a timeout or more than {@code maxRows} rows is ONE
 *       recorded {@link ReadOutcome.Unreadable} with reason SOURCE_QUERY_FAILED and the failure
 *       in its detail — the fetch procedure gives every required document type that outcome
 *       (REQ-DOC-039, ADR-DOC-007); nothing is thrown for an external failure (G6).</li>
 * </ul>
 *
 * <p>No adapter of this port writes to a host, and the port has no method that could (G3, G14).
 */
public interface DocumentSourceQueryPort {

    /** The fetch mode this adapter serves: {@link FetchMode#PATH} or {@link FetchMode#BLOB}. */
    FetchMode mode();

    /**
     * Runs the document source query once.
     *
     * @param query      the query as the service definition stores it, with the request number
     *                   to bind, the columns to read and the row limit
     * @param connection the settings of the connection the query names, as REG supplied them
     *                   (CON-REG-011); {@code null} when REG answered "not found" — then the
     *                   query is refused under RULE-DOC-007 and nothing is sent
     * @param deadline   the Check's deadline (REQ-DOC-040): the query is bounded by the time
     *                   remaining until it
     * @return the rows, in the host's order, as an unmodifiable list — empty when the host
     *         listed nothing; or ONE UNREADABLE outcome with reason SOURCE_QUERY_FAILED
     */
    ReadOutcome<List<DocumentSourceRow>> run(DocumentSourceQuery query, ConnectionSettings connection, Instant deadline);
}
