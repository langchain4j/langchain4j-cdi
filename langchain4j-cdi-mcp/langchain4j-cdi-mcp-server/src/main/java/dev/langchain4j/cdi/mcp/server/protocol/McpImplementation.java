package dev.langchain4j.cdi.mcp.server.protocol;

/** Server implementation info returned during MCP initialization. */
public record McpImplementation(String name, String version) {}
