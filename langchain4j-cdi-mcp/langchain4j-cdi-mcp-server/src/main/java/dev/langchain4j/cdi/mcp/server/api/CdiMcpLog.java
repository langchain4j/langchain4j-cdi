package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import org.mcp_java.server.McpLog;

/** Implementation of {@link McpLog} that delegates to our internal {@link McpLogger}. */
public class CdiMcpLog implements McpLog {

    private final McpLogger mcpLogger;
    private final String loggerName;

    public CdiMcpLog(McpLogger mcpLogger, String loggerName) {
        this.mcpLogger = mcpLogger;
        this.loggerName = loggerName;
    }

    @Override
    public LogLevel level() {
        return toApiLevel(mcpLogger.getMinimumLevel());
    }

    @Override
    public void send(LogLevel level, Object data) {
        mcpLogger.log(toInternalLevel(level), loggerName, data != null ? data.toString() : "null");
    }

    @Override
    public void send(LogLevel level, String format, Object... params) {
        mcpLogger.log(toInternalLevel(level), loggerName, formatMessage(format, params));
    }

    @Override
    public void debug(String format, Object... params) {
        send(LogLevel.DEBUG, format, params);
    }

    @Override
    public void info(String format, Object... params) {
        send(LogLevel.INFO, format, params);
    }

    @Override
    public void error(String format, Object... params) {
        send(LogLevel.ERROR, format, params);
    }

    @Override
    public void error(Throwable throwable, String format, Object... params) {
        String message = formatMessage(format, params) + " - " + throwable.getMessage();
        send(LogLevel.ERROR, message);
    }

    private static McpLogLevel toInternalLevel(LogLevel level) {
        return McpLogLevel.values()[level.ordinal()];
    }

    private static LogLevel toApiLevel(McpLogLevel level) {
        return LogLevel.values()[level.ordinal()];
    }

    private static String formatMessage(String format, Object... params) {
        if (params == null || params.length == 0) {
            return format;
        }
        String result = format;
        for (Object param : params) {
            int idx = result.indexOf("{}");
            if (idx >= 0) {
                result = result.substring(0, idx) + param + result.substring(idx + 2);
            } else {
                break;
            }
        }
        return result;
    }
}
