package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import org.mcp_java.model.lifecycle.InitializeRequest;
import org.mcp_java.server.McpConnection;
import org.mcp_java.server.McpLog;

/** Implementation of {@link McpConnection} that delegates to {@link McpSession} and {@link McpLogger}. */
public class CdiMcpConnection implements McpConnection {

    private final McpSession session;
    private final McpLogger mcpLogger;

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

    @Override
    public InitializeRequest initialRequest() {
        return null;
    }

    @Override
    public McpLog.LogLevel logLevel() {
        McpLogLevel level = mcpLogger.getMinimumLevel();
        return McpLog.LogLevel.values()[level.ordinal()];
    }
}
