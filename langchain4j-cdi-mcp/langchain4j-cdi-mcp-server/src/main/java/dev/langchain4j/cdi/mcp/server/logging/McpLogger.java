package dev.langchain4j.cdi.mcp.server.logging;

import dev.langchain4j.cdi.mcp.server.transport.McpNotificationBroadcaster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

/** Injectable MCP logger that sends log notifications to connected clients via {@code notifications/message}. */
@ApplicationScoped
public class McpLogger {

    @Inject
    McpNotificationBroadcaster broadcaster;

    private volatile McpLogLevel minimumLevel = McpLogLevel.debug;

    public void setMinimumLevel(McpLogLevel level) {
        this.minimumLevel = level;
    }

    public McpLogLevel getMinimumLevel() {
        return minimumLevel;
    }

    public void debug(String loggerName, String message) {
        log(McpLogLevel.debug, loggerName, message);
    }

    public void info(String loggerName, String message) {
        log(McpLogLevel.info, loggerName, message);
    }

    public void warning(String loggerName, String message) {
        log(McpLogLevel.warning, loggerName, message);
    }

    public void error(String loggerName, String message) {
        log(McpLogLevel.error, loggerName, message);
    }

    public void log(McpLogLevel level, String loggerName, String message) {
        if (level.ordinal() < minimumLevel.ordinal()) {
            return;
        }
        if (broadcaster == null || broadcaster.connectedStreamCount() == 0) {
            return;
        }
        Map<String, Object> notification = Map.of(
                "jsonrpc", "2.0",
                "method", "notifications/message",
                "params",
                        Map.of(
                                "level", level.name(),
                                "logger", loggerName,
                                "data", message));
        broadcaster.broadcast(notification);
    }
}
