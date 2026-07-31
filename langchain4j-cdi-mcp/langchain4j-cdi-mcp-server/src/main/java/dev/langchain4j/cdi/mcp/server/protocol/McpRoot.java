package dev.langchain4j.cdi.mcp.server.protocol;

/** Wire-format for an MCP file root. */
public record McpRoot(String uri, String name) {

    public static McpRoot of(String uri, String name) {
        return new McpRoot(uri, name);
    }
}
