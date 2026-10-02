package io.agenticai.chk.adapter;

import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.QueryResult;
import io.agenticai.chk.port.QueryResult.NotRead;
import io.agenticai.chk.port.QueryResult.Rows;
import io.agenticai.chk.port.ServiceQuery;
import io.agenticai.chk.port.ServiceQueryPort;
import io.agenticai.platform.mcp.McpQueryChannel;
import io.agenticai.reg.contract.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link ServiceQueryPort}: runs a service query through the platform MCP query channel
 * ({@link McpQueryChannel} — the platform track's query-executor bean, the Spring AI MCP client
 * calling the connection's query tool). It is the only way CHK reaches host data (REQ-CHK-013); no
 * JDBC connection is ever opened by CHK (REQ-CHK-018). The channel is taken as an
 * {@link ObjectProvider} because no delivered package defines its bean yet; without one every
 * service query is recorded as not read and nothing is called.
 *
 * <p>Every run goes through the same steps, in this order — every guard <em>before</em> sending:
 * <ol>
 *   <li>RULE-CHK-002 — connection absent (REG: not found) or not of type {@code mcp} → not read
 *       with the rule's message (REQ-CHK-014); read-only is guaranteed by REG for every supplied
 *       connection (see {@link ServiceQueryGuards});</li>
 *   <li>RULE-CHK-003 — the SQL text has no {@code :{inputName}} token → not read with the rule's
 *       message (REQ-CHK-012); the text is never changed;</li>
 *   <li>no channel bean in this deployable → not read;</li>
 *   <li>the Check's deadline already reached → not read (REQ-CHK-016);</li>
 *   <li>one call: the SQL text exactly as stored (REQ-CHK-011), the request number as the single
 *       bound value of {@code inputName} (REQ-CHK-012; G4), every column ({@code columns = null}),
 *       the limit {@code maxRows + 1} (REQ-CHK-050) and the deadline (REQ-CHK-016);</li>
 *   <li>any exception → not read with the failure text (REQ-CHK-049);</li>
 *   <li>more than {@code maxRows} rows → not read "more than {maxRows} rows", no row kept
 *       (REQ-CHK-050).</li>
 * </ol>
 *
 * <p>Failure translation (A.4.9, E.1.5): nothing escapes the port — every failure is a recorded
 * {@link NotRead}; the cause is logged at debug. Neither the SQL text, nor the request number, nor
 * any row reaches a log or a detail (E.4.3). Stateless: nothing is kept between calls (G9). Never a
 * model tool (G1).
 */
@Component
public class McpServiceQueryAdapter implements ServiceQueryPort {

    private static final Logger log = LoggerFactory.getLogger(McpServiceQueryAdapter.class);

    /** {@code columns} argument of the channel: every column the statement selects. */
    private static final List<String> ALL_COLUMNS = null;

    private final ObjectProvider<McpQueryChannel> channel;

    public McpServiceQueryAdapter(ObjectProvider<McpQueryChannel> channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    @Override
    public QueryResult run(ServiceQuery query, ConnectionSettings connection, Instant deadline) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(deadline, "deadline");

        // 1. RULE-CHK-002: only a read-only mcp connection (REQ-CHK-014)
        NotRead refused = ServiceQueryGuards.refuseUnlessReadOnlyMcp(query, connection);
        if (refused != null) {
            return refused;
        }
        // 2. RULE-CHK-003: the request number only as the bound parameter (REQ-CHK-012)
        refused = ServiceQueryGuards.refuseUnlessBindParameter(query);
        if (refused != null) {
            return refused;
        }
        // 3. the platform channel must be present in this deployable
        McpQueryChannel queryChannel = channel.getIfAvailable();
        if (queryChannel == null) {
            return notRead("CHK-DETAIL-QUERY-CHANNEL-ABSENT");
        }
        // 4. none of the Check's time left → nothing sent (REQ-CHK-016)
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            return notRead("CHK-DETAIL-QUERY-OUT-OF-TIME");
        }
        // 5. one call: stored text, one bound value, all columns, maxRows + 1, the deadline
        log.debug("CHK service query \"{}\" over connection \"{}\": sending through the MCP channel, "
                        + "limit {} rows, {} ms left",
                query.queryName(), connection.connectionName(), query.rowLimit(), remaining.toMillis());
        List<Map<String, Object>> rows;
        try {
            rows = queryChannel.execute(connection, query.sqlText(), query.inputName(), query.requestNumber(),
                    ALL_COLUMNS, query.rowLimit(), deadline);
        } catch (Exception e) {
            // 6. translation only: the channel's failure becomes the query's recorded outcome (REQ-CHK-049)
            String failure = ServiceQueryGuards.failureText(e, query.sqlText());
            log.debug("CHK service query \"{}\" over connection \"{}\" failed: {}",
                    query.queryName(), connection.connectionName(), failure, e);
            return new NotRead(failure);
        }
        if (rows == null) {
            return notRead("CHK-DETAIL-QUERY-NO-RESULT");
        }
        // 7. over the limit → not read, none of its rows kept (REQ-CHK-050)
        if (rows.size() > query.maxRows()) {
            return new NotRead(CheckEngineTexts.english("CHK-DETAIL-QUERY-OVER-ROW-LIMIT", query.maxRows()));
        }
        log.debug("CHK service query \"{}\": {} row(s) read", query.queryName(), rows.size());
        return new Rows(rows);
    }

    private static NotRead notRead(String key) {
        return new NotRead(CheckEngineTexts.english(key));
    }
}
