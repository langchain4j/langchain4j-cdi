package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Wire-format for the MCP resources/list response. */
public record McpListResourcesResult(List<McpResourceModel> resources, McpCursor nextCursor) {}
