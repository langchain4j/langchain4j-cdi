package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Wire-format for the MCP resources/templates/list response. */
public record McpListResourceTemplatesResult(List<McpResourceTemplateModel> resourceTemplates, McpCursor nextCursor) {}
