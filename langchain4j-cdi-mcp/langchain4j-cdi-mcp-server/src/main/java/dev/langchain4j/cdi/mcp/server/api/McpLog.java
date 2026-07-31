package dev.langchain4j.cdi.mcp.server.api;

/** Logging interface for MCP tool/prompt/resource methods. */
public interface McpLog {

    /** Log severity levels matching the MCP specification ordering. */
    enum LogLevel {
        DEBUG,
        INFO,
        NOTICE,
        WARNING,
        ERROR,
        CRITICAL,
        ALERT,
        EMERGENCY
    }

    LogLevel level();

    void send(LogLevel level, Object data);

    void send(LogLevel level, String format, Object... params);

    void debug(String format, Object... params);

    void info(String format, Object... params);

    void error(String format, Object... params);

    void error(Throwable throwable, String format, Object... params);
}
