package dev.langchain4j.cdi.mcp.server.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpExceptionTest {

    @Test
    void shouldPreserveStringRequestId() {
        McpException ex = new McpException("req-1", McpErrorCode.INTERNAL_ERROR, "oops");

        assertThat(ex.getRequestId()).isEqualTo("req-1");
        assertThat(ex.getErrorCode()).isEqualTo(McpErrorCode.INTERNAL_ERROR);
        assertThat(ex.getMessage()).isEqualTo("oops");
    }

    @Test
    void shouldPreserveNumericRequestId() {
        McpException ex = new McpException(42L, McpErrorCode.TOOL_NOT_FOUND, "not found");

        assertThat(ex.getRequestId()).isEqualTo(42L);
        assertThat(ex.getRequestId()).isInstanceOf(Long.class);
    }

    @Test
    void shouldHandleNullRequestId() {
        McpException ex = new McpException(null, McpErrorCode.PARSE_ERROR, "bad");

        assertThat(ex.getRequestId()).isNull();
    }
}
