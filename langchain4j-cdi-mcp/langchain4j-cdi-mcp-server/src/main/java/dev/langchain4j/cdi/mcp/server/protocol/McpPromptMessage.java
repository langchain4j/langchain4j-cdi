package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.ServiceLoader;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.spi.McpServerSPI;

/**
 * A message within an MCP prompt response. Each message has a role ("user" or "assistant") and content. Can be returned
 * from {@link org.mcpjava.server.prompts.Prompt @Prompt} methods as {@code List<McpPromptMessage>}.
 *
 * <p>This type exists alongside the SPI-backed {@link org.mcpjava.server.prompts.PromptMessage} to provide convenient
 * String-role construction matching the MCP protocol wire format.
 */
public record McpPromptMessage(String role, ContentBlock content) {

    private static final McpServerSPI SPI = ServiceLoader.load(McpServerSPI.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No McpServerSPI implementation found on the classpath"));

    public static McpPromptMessage user(String text) {
        return new McpPromptMessage("user", SPI.textContentBuilder(text).build());
    }

    public static McpPromptMessage assistant(String text) {
        return new McpPromptMessage("assistant", SPI.textContentBuilder(text).build());
    }
}
