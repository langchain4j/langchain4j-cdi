package dev.langchain4j.cdi.mcp.server.protocol;

/** Wire-format DTO for MCP resource listings, decoupled from the upstream {@code org.mcpjava} model types. */
public record McpResourceModel(String uri, String name, String description, String mimeType) {

    public static McpResourceModel of(String uri, String name, String description, String mimeType) {
        return new McpResourceModel(uri, name, description, mimeType);
    }
}
