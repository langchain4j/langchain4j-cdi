package dev.langchain4j.cdi.mcp.server.protocol;

/** Wire-format DTO for MCP resource template listings, decoupled from the upstream {@code org.mcpjava} model types. */
public record McpResourceTemplateModel(String uriTemplate, String name, String description, String mimeType) {

    public static McpResourceTemplateModel of(String uriTemplate, String name, String description, String mimeType) {
        return new McpResourceTemplateModel(uriTemplate, name, description, mimeType);
    }
}
