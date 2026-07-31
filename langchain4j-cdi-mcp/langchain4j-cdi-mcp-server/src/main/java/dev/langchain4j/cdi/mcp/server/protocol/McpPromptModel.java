package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Wire-format DTO for MCP prompt listings, decoupled from the upstream {@code org.mcpjava} model types. */
public record McpPromptModel(String name, String description, List<McpPromptArgument> arguments) {

    public static McpPromptModel of(String name, String description, List<McpPromptArgument> arguments) {
        return new McpPromptModel(name, description, arguments);
    }
}
