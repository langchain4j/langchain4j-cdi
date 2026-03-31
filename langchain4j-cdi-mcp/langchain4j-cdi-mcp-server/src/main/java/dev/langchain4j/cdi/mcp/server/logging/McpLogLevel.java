package dev.langchain4j.cdi.mcp.server.logging;

/** MCP log levels as defined by the MCP specification, ordered from least to most severe. */
public enum McpLogLevel {
    debug,
    info,
    notice,
    warning,
    error,
    critical,
    alert,
    emergency
}
