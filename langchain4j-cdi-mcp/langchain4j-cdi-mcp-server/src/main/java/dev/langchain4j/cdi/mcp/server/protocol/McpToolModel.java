package dev.langchain4j.cdi.mcp.server.protocol;

import jakarta.json.JsonObject;

/** Wire-format DTO for MCP tool listings, decoupled from the upstream {@code org.mcpjava} model types. */
public record McpToolModel(
        String name,
        Object annotations,
        String description,
        JsonObject inputSchema,
        Object outputSchema,
        Object returnDirect,
        Object destructive) {}
