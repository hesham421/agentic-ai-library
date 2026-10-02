package io.agenticai.platform.mcp;

import io.agenticai.reg.contract.ConnectionSettings;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The platform MCP query channel — a <strong>platform-track port</strong>, owned by no module: the
 * "query-executor bean of the platform track", i.e. the Spring AI MCP <em>client</em> calling a
 * connection's query tool. It is the one channel through which host data is read over an
 * {@code mcp} connection, consumed by Document Access (DOC — the {@code path} document source
 * query, ADR-DOC-001) and by the Check Engine (CHK — the service queries, PORTS-QUERY,
 * REQ-CHK-013).
 *
 * <p><b>Interim implementation:</b> {@link SpringAiMcpQueryChannel} (Spring AI MCP client; endpoint
 * convention {@code mcp:<client-name>}; query-tool arguments {@code {sql, binds, maxRows}} taken from
 * the local Oracle MCP server). No delivered package defines this bean and no plan or contract states
 * the query tool's argument protocol — the factory is to confirm it (OPEN {@code api_doc_gaps} rows
 * of DOC and CHK). Each consumer still takes the channel as an {@code ObjectProvider} and, while no
 * bean is present, records every query as not read without any call.
 *
 * <p>The channel is reached behind a module's query port only — it is never a model tool (G1); it
 * carries bound values only, never SQL built from free text (G4); and it never edits the SQL text.
 */
public interface McpQueryChannel {

    /**
     * Runs one SELECT through the connection's query tool and returns the requested columns of
     * each row.
     *
     * @param connection the {@code mcp} connection's settings (CON-REG-011): endpoint and query
     *                   tool; the credential is resolved by the channel from its reference
     * @param sqlText    the statement, sent exactly as given — never edited by the channel
     * @param bindName   the name of the statement's single bind parameter, without the leading
     *                   {@code :}
     * @param bindValue  the value bound to it — passed as a parameter, never put into the text
     * @param columns    the columns to return of each row; the channel returns no other column.
     *                   {@code null} means every column the statement selects — used by CHK's
     *                   service queries, whose whole result goes to the comparison (DOC always
     *                   names its columns, so no document content travels through MCP — REQ-DOC-016)
     * @param maxRows    the most rows to return
     * @param deadline   the instant by which the call must have answered
     * @return one map per returned row, keyed by column name as the host names it, in the
     *         host's order; a column's value is {@code null} when the host returned NULL
     * @throws RuntimeException any failure of the call — refused tool, transport error, the
     *                          deadline reached; the consumer's adapter translates it into a
     *                          recorded outcome
     */
    List<Map<String, Object>> execute(ConnectionSettings connection,
                                      String sqlText,
                                      String bindName,
                                      String bindValue,
                                      List<String> columns,
                                      int maxRows,
                                      Instant deadline);
}
