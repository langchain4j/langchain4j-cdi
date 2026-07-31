package dev.langchain4j.cdi.mcp.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mcpjava.server.content.TextContent;

class McpPromptMessageTest {

    @Test
    void shouldCreateUserMessage() {
        McpPromptMessage msg = McpPromptMessage.user("Hello");

        assertThat(msg.role()).isEqualTo("user");
        assertThat(msg.content()).isInstanceOf(TextContent.class);
        assertThat(((TextContent) msg.content()).text()).isEqualTo("Hello");
    }

    @Test
    void shouldCreateAssistantMessage() {
        McpPromptMessage msg = McpPromptMessage.assistant("Hi there");

        assertThat(msg.role()).isEqualTo("assistant");
        assertThat(msg.content()).isInstanceOf(TextContent.class);
        assertThat(((TextContent) msg.content()).text()).isEqualTo("Hi there");
    }
}
