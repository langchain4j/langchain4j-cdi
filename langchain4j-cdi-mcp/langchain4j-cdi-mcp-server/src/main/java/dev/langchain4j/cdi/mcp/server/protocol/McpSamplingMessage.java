package dev.langchain4j.cdi.mcp.server.protocol;

/** Wire-format for a sampling message in MCP sampling requests. */
public record McpSamplingMessage(String role, Object content) {}
