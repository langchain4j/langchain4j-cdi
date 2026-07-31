package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Wire-format for the MCP prompts/list response. */
public record McpListPromptsResult(List<McpPromptModel> prompts, McpCursor nextCursor) {}
