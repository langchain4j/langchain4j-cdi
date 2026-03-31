package dev.langchain4j.cdi.mcp.server.protocol;

import org.mcp_java.model.content.TextContent;

/**
 * A message within an MCP prompt response. Each message has a role ("user" or "assistant") and content. Can be returned
 * from {@link org.mcp_java.annotations.prompts.Prompt @Prompt} methods as {@code List<McpPromptMessage>}.
 *
 * <p>This class is kept instead of using {@code org.mcp_java.model.prompt.PromptMessage} because the mcp-model
 * {@code Role} enum serializes as "USER"/"ASSISTANT" via JSON-B, but the MCP protocol expects lowercase
 * "user"/"assistant".
 */
public class McpPromptMessage {

    private String role;
    private TextContent content;

    public McpPromptMessage() {}

    public McpPromptMessage(String role, TextContent content) {
        this.role = role;
        this.content = content;
    }

    public static McpPromptMessage user(String text) {
        return new McpPromptMessage("user", TextContent.of(text));
    }

    public static McpPromptMessage assistant(String text) {
        return new McpPromptMessage("assistant", TextContent.of(text));
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public TextContent getContent() {
        return content;
    }

    public void setContent(TextContent content) {
        this.content = content;
    }
}
