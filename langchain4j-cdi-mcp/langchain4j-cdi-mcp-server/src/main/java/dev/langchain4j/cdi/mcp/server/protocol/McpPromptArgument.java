package dev.langchain4j.cdi.mcp.server.protocol;

/** Wire-format prompt argument descriptor for MCP prompt listings. */
public record McpPromptArgument(String name, String description, boolean required) {}
