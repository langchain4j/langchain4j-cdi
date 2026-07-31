package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import jakarta.json.JsonObject;

/** Implementation of {@link McpConnection} that delegates to {@link McpSession} and {@link McpLogger}. */
public class CdiMcpConnection implements McpConnection {

    private final McpSession session;
    private final McpLogger mcpLogger;

    /**
     * Creates a new MCP connection wrapper.
     *
     * @param session the MCP session
     * @param mcpLogger the MCP logger
     */
    public CdiMcpConnection(McpSession session, McpLogger mcpLogger) {
        this.session = session;
        this.mcpLogger = mcpLogger;
    }

    @Override
    public String id() {
        return session.getId();
    }

    @Override
    public Status status() {
        return session.isInitialized() ? Status.IN_OPERATION : Status.INITIALIZING;
    }

    /**
     * Returns the MCP client's advertised capabilities from the {@code initialize} handshake.
     *
     * @return the client capabilities object, or {@code null} if the session has not yet been initialized
     */
    @Override
    public JsonObject initialRequest() {
        return session.getClientCapabilities();
    }

    @Override
    public McpLog.LogLevel logLevel() {
        McpLogLevel level = mcpLogger.getMinimumLevel();
        return McpLog.LogLevel.values()[level.ordinal()];
    }
}
