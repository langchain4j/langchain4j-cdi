package dev.langchain4j.cdi.mcp.server.protocol;

/** Pagination cursor for MCP list responses. */
public record McpCursor(String cursor) {}
