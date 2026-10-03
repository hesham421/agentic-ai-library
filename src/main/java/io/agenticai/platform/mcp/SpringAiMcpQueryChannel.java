package io.agenticai.platform.mcp;

import io.agenticai.reg.contract.ConnectionSettings;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The platform MCP query channel over the Spring AI MCP <em>client</em> — interim implementation of
 * {@link McpQueryChannel} (see the OPEN {@code api_doc_gaps} rows of DOC and CHK: the factory has
 * not stated the query tool's argument protocol; this class takes it from the local Oracle MCP
 * server, {@code governance/mcp-servers/oracle/index.js}).
 *
 * <p><b>Endpoint convention.</b> A REG {@code mcp} connection's {@code endpoint} is
 * {@code mcp:<client-name>}, where {@code <client-name>} is the name of a Spring AI MCP client
 * connection configured under {@code spring.ai.mcp.client.*} (e.g.
 * {@code spring.ai.mcp.client.stdio.connections.<client-name>.command}). The Spring AI client keeps
 * that name as its client-info {@code title}; the channel resolves the client by it on every call.
 * The connection's credential is held by the MCP server's own configuration — the channel never
 * reads or forwards the {@code credentialReference}.
 *
 * <p><b>Query-tool argument protocol.</b> One {@code tools/call} of the connection's
 * {@code queryTool} with {@code {sql: <the stored text, exactly>, binds: {<bindName>: <bindValue>},
 * maxRows: <maxRows as given>}}; the tool answers one text content holding the JSON
 * {@code {rowCount, rows: [{COLUMN: value, …}]}}. A result flagged {@code isError} is a failure
 * carrying the tool's text.
 *
 * <p><b>Guardrails.</b> The channel is reached only behind DOC's and CHK's query ports — it is never
 * a model tool (G1): the Spring AI MCP→ToolCallback bridge is switched off
 * ({@code spring.ai.mcp.client.toolcallback.enabled=false}) and {@link #verifyNoModelToolBridge}
 * refuses start-up if any {@link ToolCallbackProvider} bean exists. The SQL text is sent unchanged
 * and the value only as a bind (G4); the server is read-only (G3). Nothing is kept between calls
 * (G9). The log carries names, counts and timings — never SQL, bound values or rows.
 */
@Component
public class SpringAiMcpQueryChannel implements McpQueryChannel {

    private static final Logger log = LoggerFactory.getLogger(SpringAiMcpQueryChannel.class);

    /** The endpoint prefix naming a Spring AI MCP client connection. */
    public static final String ENDPOINT_PREFIX = "mcp:";

    static final String ARG_SQL = "sql";
    static final String ARG_BINDS = "binds";
    static final String ARG_MAX_ROWS = "maxRows";
    static final String RESULT_ROWS = "rows";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ObjectProvider<List<McpSyncClient>> clients;
    private final ListableBeanFactory beanFactory;
    private final ExecutorService calls = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * One lock per MCP client: the stdio transport of the MCP client accepts one outbound message at a time
     * (two concurrent {@code callTool}s on the same client fail one of them with "Failed to enqueue message",
     * seen with two concurrent Checks — E2E chk-pipeline 2026-10-03), so the calls of one client are
     * serialised; each waits for the lock only within the Check's remaining time (interruptible).
     */
    private final Map<McpSyncClient, ReentrantLock> clientLocks = new ConcurrentHashMap<>();

    public SpringAiMcpQueryChannel(ObjectProvider<List<McpSyncClient>> clients, ListableBeanFactory beanFactory) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory");
    }

    /**
     * G1 at start-up: no MCP tool may ever be offered to a model. Refuses start-up when a
     * {@link ToolCallbackProvider} bean exists; logs the configured MCP clients by name.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verifyNoModelToolBridge() {
        String[] providers = beanFactory.getBeanNamesForType(ToolCallbackProvider.class, true, false);
        if (providers.length > 0) {
            throw new IllegalStateException("G1: a ToolCallbackProvider bean exists (" + String.join(", ", providers)
                    + ") — MCP tools must never be offered to a model; set spring.ai.mcp.client.toolcallback.enabled=false");
        }
        List<String> names = configuredClients().stream().map(SpringAiMcpQueryChannel::clientName).toList();
        log.info("Platform MCP query channel ready: {} MCP client(s) {}; ToolCallbackProvider beans: 0 "
                + "(MCP tools are never offered to a model — G1)", names.size(), names);
    }

    @Override
    public List<Map<String, Object>> execute(ConnectionSettings connection, String sqlText, String bindName,
                                             String bindValue, List<String> columns, int maxRows, Instant deadline) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(sqlText, "sqlText");
        Objects.requireNonNull(bindName, "bindName");
        Objects.requireNonNull(deadline, "deadline");
        if (connection.queryTool() == null || connection.queryTool().isBlank()) {
            throw new McpQueryChannelException("connection \"" + connection.connectionName() + "\" names no query tool");
        }
        McpSyncClient client = resolveClient(connection);

        Map<String, Object> binds = new LinkedHashMap<>();
        binds.put(bindName, bindValue);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put(ARG_SQL, sqlText);
        arguments.put(ARG_BINDS, binds);
        arguments.put(ARG_MAX_ROWS, maxRows);
        CallToolRequest request = CallToolRequest.builder(connection.queryTool()).arguments(arguments).build();

        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isNegative() || remaining.isZero()) {
            throw new McpQueryChannelException("the deadline was reached before the query tool was called");
        }
        long started = System.nanoTime();
        CallToolResult result = callWithin(client, request, remaining, connection);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        String text = textOf(result);
        if (Boolean.TRUE.equals(result.isError())) {
            log.info("MCP query channel: tool \"{}\" of connection \"{}\" answered an error in {} ms",
                    connection.queryTool(), connection.connectionName(), elapsedMs);
            throw new McpQueryChannelException("the query tool \"" + connection.queryTool() + "\" refused the call: "
                    + (text == null ? "(no text)" : text));
        }
        List<Map<String, Object>> rows = parseRows(text, connection);
        log.info("MCP query channel: tool \"{}\" of connection \"{}\" answered {} row(s) in {} ms",
                connection.queryTool(), connection.connectionName(), rows.size(), elapsedMs);
        return columns == null ? rows : project(rows, columns);
    }

    private McpSyncClient resolveClient(ConnectionSettings connection) {
        String endpoint = connection.endpoint();
        if (endpoint == null || !endpoint.startsWith(ENDPOINT_PREFIX)
                || endpoint.length() == ENDPOINT_PREFIX.length()) {
            throw new McpQueryChannelException("endpoint of connection \"" + connection.connectionName()
                    + "\" is not of the form " + ENDPOINT_PREFIX + "<client-name>");
        }
        String clientName = endpoint.substring(ENDPOINT_PREFIX.length()).trim();
        for (McpSyncClient client : configuredClients()) {
            if (clientName.equals(clientName(client))) {
                return client;
            }
        }
        throw new McpQueryChannelException("no MCP client named \"" + clientName + "\" is configured "
                + "(spring.ai.mcp.client.*.connections." + clientName + ")");
    }

    private List<McpSyncClient> configuredClients() {
        List<McpSyncClient> configured = clients.getIfAvailable(List::of);
        return configured == null ? List.of() : configured;
    }

    /** The Spring AI client connection name: kept as the client-info title (McpClientAutoConfiguration). */
    private static String clientName(McpSyncClient client) {
        return client.getClientInfo() == null ? null : client.getClientInfo().title();
    }

    private CallToolResult callWithin(McpSyncClient client, CallToolRequest request, Duration remaining,
                                      ConnectionSettings connection) {
        ReentrantLock lock = clientLocks.computeIfAbsent(client, unused -> new ReentrantLock());
        Future<CallToolResult> call = calls.submit(() -> {
            lock.lockInterruptibly();
            try {
                return client.callTool(request);
            } finally {
                lock.unlock();
            }
        });
        try {
            CallToolResult result = call.get(remaining.toMillis(), TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new McpQueryChannelException("the query tool returned no result");
            }
            return result;
        } catch (TimeoutException e) {
            call.cancel(true);
            throw new McpQueryChannelException("the query tool of connection \"" + connection.connectionName()
                    + "\" did not answer before the Check's deadline", e);
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw new McpQueryChannelException("the query call was interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof McpQueryChannelException channelFailure) {
                throw channelFailure;
            }
            throw new McpQueryChannelException("the MCP call failed: " + cause.getClass().getSimpleName()
                    + (cause.getMessage() == null ? "" : ": " + cause.getMessage()), cause);
        }
    }

    private static String textOf(CallToolResult result) {
        if (result.content() == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (Content content : result.content()) {
            if (content instanceof TextContent textContent && textContent.text() != null) {
                text.append(textContent.text());
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseRows(String text, ConnectionSettings connection) {
        if (text == null) {
            throw new McpQueryChannelException("the query tool of connection \"" + connection.connectionName()
                    + "\" answered no text result");
        }
        Object parsed;
        try {
            parsed = JSON.readValue(text, Object.class);
        } catch (JacksonException e) {
            throw new McpQueryChannelException("the query tool's answer is not JSON {rowCount, rows}", e);
        }
        if (!(parsed instanceof Map<?, ?> body) || !(body.get(RESULT_ROWS) instanceof List<?> rowList)) {
            throw new McpQueryChannelException("the query tool's answer holds no \"rows\" array");
        }
        List<Map<String, Object>> rows = new ArrayList<>(rowList.size());
        for (Object row : rowList) {
            if (!(row instanceof Map<?, ?> rowMap)) {
                throw new McpQueryChannelException("a row of the query tool's answer is not an object");
            }
            rows.add(new LinkedHashMap<>((Map<String, Object>) rowMap));
        }
        return rows;
    }

    /** Only the named columns of each row, matched case-insensitively, keyed as the host names them. */
    private static List<Map<String, Object>> project(List<Map<String, Object>> rows, List<String> columns) {
        List<Map<String, Object>> projected = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> kept = new LinkedHashMap<>();
            for (String column : columns) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(column)) {
                        kept.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }
            projected.add(kept);
        }
        return projected;
    }

    /** A failure of the channel; the consuming adapter translates it into a recorded outcome. */
    public static final class McpQueryChannelException extends RuntimeException {

        public McpQueryChannelException(String message) {
            super(message);
        }

        public McpQueryChannelException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
