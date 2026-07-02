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

    /** The message role ({@code "user"} or {@code "assistant"}). */
    private String role;

    /** The text content of this message. */
    private TextContent content;

    /** Default constructor for JSON-B deserialization. */
    public McpPromptMessage() {}

    /**
     * Creates a prompt message with the given role and content.
     *
     * @param role the role ({@code "user"} or {@code "assistant"})
     * @param content the text content
     */
    public McpPromptMessage(String role, TextContent content) {
        this.role = role;
        this.content = content;
    }

    /**
     * Creates a user message with the given text.
     *
     * @param text the message text
     * @return a new user prompt message
     */
    public static McpPromptMessage user(String text) {
        return new McpPromptMessage("user", TextContent.of(text));
    }

    /**
     * Creates an assistant message with the given text.
     *
     * @param text the message text
     * @return a new assistant prompt message
     */
    public static McpPromptMessage assistant(String text) {
        return new McpPromptMessage("assistant", TextContent.of(text));
    }

    /**
     * Returns the message role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the message role.
     *
     * @param role the role
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Returns the message content.
     *
     * @return the text content
     */
    public TextContent getContent() {
        return content;
    }

    /**
     * Sets the message content.
     *
     * @param content the text content
     */
    public void setContent(TextContent content) {
        this.content = content;
    }
}
