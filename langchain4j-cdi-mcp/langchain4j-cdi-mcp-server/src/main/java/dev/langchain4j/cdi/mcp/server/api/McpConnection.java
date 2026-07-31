package dev.langchain4j.cdi.mcp.server.api;

import jakarta.json.JsonObject;

/** Provides MCP session connection information. */
public interface McpConnection {

    /** Connection lifecycle status. */
    enum Status {
        INITIALIZING,
        IN_OPERATION
    }

    String id();

    Status status();

    JsonObject initialRequest();

    McpLog.LogLevel logLevel();
}
