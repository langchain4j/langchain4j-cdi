package dev.langchain4j.cdi.mcp.server.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpSessionExceptionTest {

    @Test
    void shouldCreateWithCorrectErrorCode() {
        McpSessionException ex = new McpSessionException("req-1", "session not found");

        assertThat(ex.getErrorCode()).isEqualTo(McpErrorCode.SESSION_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("session not found");
    }

    @Test
    void shouldCreateWithNumericId() {
        McpSessionException ex = new McpSessionException(5L, "missing");

        assertThat(ex.getRequestId()).isEqualTo(5L);
    }
}
