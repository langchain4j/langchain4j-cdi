package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mcp_java.server.Icon;
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
    public String protocolVersion() {
        return "2025-03-26";
    }

    @Override
    public Map<String, Object> rawClientCapabilities() {
        return Map.of();
    }

    @Override
    public ImplementationInfo clientInfo() {
        return new ImplementationInfo() {
            @Override
            public List<Icon> icons() {
                return List.of();
            }

            @Override
            public String name() {
                return "";
            }

            @Override
            public String title() {
                return "";
            }

            @Override
            public String version() {
                return "";
            }

            @Override
            public Optional<String> description() {
                return Optional.empty();
            }

            @Override
            public Optional<String> websiteUrl() {
                return Optional.empty();
            }
        };
    }

    @Override
    public McpLog.LogLevel logLevel() {
        McpLogLevel level = mcpLogger.getMinimumLevel();
        return McpLog.LogLevel.values()[level.ordinal()];
    }
}
