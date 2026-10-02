package io.agenticai.chk.port;

import io.agenticai.reg.contract.ConnectionSettings;

import java.time.Instant;

/**
 * The Check Engine's only way to host data (REQ-CHK-013, REQ-CHK-018): runs one service query of
 * the Check's version through the platform MCP query channel. CHK opens no JDBC connection itself.
 * No implementation of this port is ever handed to the comparison model as a tool (G1).
 *
 * <p>Contract: the SQL text is sent exactly as stored (REQ-CHK-011); the request number is passed
 * as ONE bound value under {@code :{inputName}}, never concatenated, whatever characters it holds
 * (REQ-CHK-012; AIAS-5); the limit {@code maxRows + 1} is requested so an over-limit result is
 * detected, never truncated (REQ-CHK-050); the call's timeout is the time left before
 * {@code deadline} (REQ-CHK-016). Every failure is a {@link QueryResult.NotRead}, never thrown.
 */
public interface ServiceQueryPort {

    /**
     * Runs one service query.
     *
     * @param query      the query, its bound value and the row limit
     * @param connection the settings of the connection the query names (CON-REG-011, the connection of
     *                   CON-REG-005); {@code null} when REG reported it not found (not registered)
     * @param deadline   the instant the Check times out
     * @return the rows, or not read with a detail
     */
    QueryResult run(ServiceQuery query, ConnectionSettings connection, Instant deadline);
}
