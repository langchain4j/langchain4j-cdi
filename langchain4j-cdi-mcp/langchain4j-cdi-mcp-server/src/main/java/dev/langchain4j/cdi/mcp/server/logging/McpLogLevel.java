package dev.langchain4j.cdi.mcp.server.logging;

/** MCP log levels as defined by the MCP specification, ordered from least to most severe. */
public enum McpLogLevel {
    /** Detailed debugging information. */
    debug,
    /** Informational messages. */
    info,
    /** Normal but noteworthy conditions. */
    notice,
    /** Warning conditions that may require attention. */
    warning,
    /** Error conditions. */
    error,
    /** Critical conditions requiring immediate action. */
    critical,
    /** Action must be taken immediately. */
    alert,
    /** System is unusable. */
    emergency
}
