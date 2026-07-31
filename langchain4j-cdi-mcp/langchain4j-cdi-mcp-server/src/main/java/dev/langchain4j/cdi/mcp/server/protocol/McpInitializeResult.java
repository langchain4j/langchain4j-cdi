package dev.langchain4j.cdi.mcp.server.protocol;

/** Wire-format for the MCP initialize response. */
public record McpInitializeResult(
        String protocolVersion, McpServerCapabilities capabilities, McpImplementation serverInfo) {}
