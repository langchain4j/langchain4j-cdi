package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

/** Result of an MCP {@code prompts/get} request, containing a description and a list of messages. */
public class McpPromptGetResult {

    /** Human-readable description of the prompt. */
    private String description;

    /** Ordered list of messages that make up the prompt. */
    private List<McpPromptMessage> messages;

    /** Default constructor for JSON-B deserialization. */
    public McpPromptGetResult() {}

    /**
     * Creates a result with the given description and messages.
     *
     * @param description the prompt description
     * @param messages the prompt messages
     */
    public McpPromptGetResult(String description, List<McpPromptMessage> messages) {
        this.description = description;
        this.messages = messages;
    }

    /**
     * Returns the prompt description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the prompt description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the prompt messages.
     *
     * @return the messages
     */
    public List<McpPromptMessage> getMessages() {
        return messages;
    }

    /**
     * Sets the prompt messages.
     *
     * @param messages the messages
     */
    public void setMessages(List<McpPromptMessage> messages) {
        this.messages = messages;
    }
}
