package dev.langchain4j.cdi.mcp.server.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mcpjava.server.Cancellation;
import org.mcpjava.server.progress.Progress;

class McpFrameworkTypesTest {

    @Test
    void shouldRecognizeAllFrameworkTypes() {
        assertThat(McpFrameworkTypes.isFrameworkType(McpLog.class)).isTrue();
        assertThat(McpFrameworkTypes.isFrameworkType(Progress.class)).isTrue();
        assertThat(McpFrameworkTypes.isFrameworkType(Cancellation.class)).isTrue();
        assertThat(McpFrameworkTypes.isFrameworkType(McpConnection.class)).isTrue();
        assertThat(McpFrameworkTypes.isFrameworkType(Roots.class)).isTrue();
        assertThat(McpFrameworkTypes.isFrameworkType(Sampling.class)).isTrue();
        assertThat(McpFrameworkTypes.isFrameworkType(Elicitation.class)).isTrue();
    }

    @Test
    void shouldNotRecognizeNonFrameworkTypes() {
        assertThat(McpFrameworkTypes.isFrameworkType(String.class)).isFalse();
        assertThat(McpFrameworkTypes.isFrameworkType(Integer.class)).isFalse();
        assertThat(McpFrameworkTypes.isFrameworkType(Object.class)).isFalse();
    }
}
