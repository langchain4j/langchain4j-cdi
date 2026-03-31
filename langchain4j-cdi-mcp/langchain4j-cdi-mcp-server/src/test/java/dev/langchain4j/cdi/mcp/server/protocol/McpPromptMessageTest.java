package dev.langchain4j.cdi.mcp.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpPromptMessageTest {

    @Test
    void shouldCreateUserMessage() {
        McpPromptMessage msg = McpPromptMessage.user("Hello");

        assertThat(msg.getRole()).isEqualTo("user");
        assertThat(msg.getContent().type()).isEqualTo("text");
        assertThat(msg.getContent().text()).isEqualTo("Hello");
    }

    @Test
    void shouldCreateAssistantMessage() {
        McpPromptMessage msg = McpPromptMessage.assistant("Hi there");

        assertThat(msg.getRole()).isEqualTo("assistant");
        assertThat(msg.getContent().text()).isEqualTo("Hi there");
    }
}
