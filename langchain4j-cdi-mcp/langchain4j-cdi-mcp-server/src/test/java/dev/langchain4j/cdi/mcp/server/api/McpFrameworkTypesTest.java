package dev.langchain4j.cdi.mcp.server.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mcp_java.server.Cancellation;
import org.mcp_java.server.Elicitation;
import org.mcp_java.server.McpConnection;
import org.mcp_java.server.McpLog;
import org.mcp_java.server.Progress;
import org.mcp_java.server.Roots;
import org.mcp_java.server.Sampling;

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
