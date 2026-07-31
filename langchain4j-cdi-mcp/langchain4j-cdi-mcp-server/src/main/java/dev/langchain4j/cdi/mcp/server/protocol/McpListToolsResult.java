package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Wire-format for the MCP tools/list response. */
public record McpListToolsResult(List<McpToolModel> tools, McpCursor nextCursor) {}
