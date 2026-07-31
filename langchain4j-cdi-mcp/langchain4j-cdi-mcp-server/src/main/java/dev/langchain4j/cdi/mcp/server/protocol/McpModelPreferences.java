package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Wire-format for sampling model preferences in MCP. */
public record McpModelPreferences(List<Object> hints) {}
