package dev.langchain4j.cdi.mcp.server.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpToolNotFoundExceptionTest {

    @Test
    void shouldIncludeToolNameInMessage() {
        McpToolNotFoundException ex = new McpToolNotFoundException("req-1", "myTool");

        assertThat(ex.getErrorCode()).isEqualTo(McpErrorCode.TOOL_NOT_FOUND);
        assertThat(ex.getMessage()).contains("myTool");
    }

    @Test
    void shouldAcceptNumericId() {
        McpToolNotFoundException ex = new McpToolNotFoundException(3L, "tool");

        assertThat(ex.getRequestId()).isEqualTo(3L);
    }
}
