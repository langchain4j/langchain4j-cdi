package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.List;

public class McpPromptGetResult {

    private String description;
    private List<McpPromptMessage> messages;

    public McpPromptGetResult() {}

    public McpPromptGetResult(String description, List<McpPromptMessage> messages) {
        this.description = description;
        this.messages = messages;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<McpPromptMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<McpPromptMessage> messages) {
        this.messages = messages;
    }
}
