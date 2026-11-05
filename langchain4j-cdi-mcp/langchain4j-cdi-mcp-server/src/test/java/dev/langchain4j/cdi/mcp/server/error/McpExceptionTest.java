package dev.langchain4j.cdi.mcp.server.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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

    // --- langchain4j-cdi#291: the failure an error was built from stays reachable ---

    @Test
    void shouldChainTheCause() {
        IllegalStateException root = new IllegalStateException("database unreachable");

        McpException ex = new McpException("req-1", McpErrorCode.INTERNAL_ERROR, "oops", root);

        assertThat(ex.getCause()).isSameAs(root);
        assertThat(ex.getRequestId()).isEqualTo("req-1");
        assertThat(ex.getErrorCode()).isEqualTo(McpErrorCode.INTERNAL_ERROR);
        assertThat(ex.getMessage()).isEqualTo("oops");
        assertThat(ex.getHttpStatus()).isEqualTo(200);
        assertThat(ex.getData()).isNull();
    }

    @Test
    void shouldChainTheCauseAlongWithHttpStatusAndData() {
        IllegalStateException root = new IllegalStateException("database unreachable");
        Map<String, String> data = Map.of("uri", "file:///nope");

        McpException ex = new McpException(42L, McpErrorCode.INVALID_PARAMS, "Resource not found", 400, data, root);

        assertThat(ex.getCause()).isSameAs(root);
        assertThat(ex.getHttpStatus()).isEqualTo(400);
        assertThat(ex.getData()).isEqualTo(data);
    }

    @Test
    void shouldHaveNoCauseWhenNoneIsGiven() {
        assertThat(new McpException("req-1", McpErrorCode.INTERNAL_ERROR, "oops").getCause())
                .isNull();
        assertThat(new McpException(1, McpErrorCode.INTERNAL_ERROR, "oops", 200, null).getCause())
                .isNull();
    }
}
