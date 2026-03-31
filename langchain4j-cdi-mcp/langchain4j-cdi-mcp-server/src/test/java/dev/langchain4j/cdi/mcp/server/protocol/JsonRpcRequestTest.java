package dev.langchain4j.cdi.mcp.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonRpcRequestTest {

    @Test
    void shouldCreateWithStringId() {
        JsonRpcRequest request = new JsonRpcRequest("abc", "tools/list", null);

        assertThat(request.getId()).isEqualTo("abc");
        assertThat(request.getMethod()).isEqualTo("tools/list");
        assertThat(request.getJsonrpc()).isEqualTo("2.0");
    }

    @Test
    void shouldCreateWithNumericId() {
        JsonRpcRequest request = new JsonRpcRequest(42L, "tools/call", null);

        assertThat(request.getId()).isEqualTo(42L);
        assertThat(request.getId()).isInstanceOf(Long.class);
    }

    @Test
    void shouldCreateWithNullId() {
        JsonRpcRequest request = new JsonRpcRequest(null, "notifications/initialized", null);

        assertThat(request.getId()).isNull();
    }

    @Test
    void shouldStoreProgressToken() {
        JsonRpcRequest request = new JsonRpcRequest(1L, "tools/call", null);
        request.setProgressToken("progress-token-1");

        assertThat(request.getProgressToken()).isEqualTo("progress-token-1");
    }
}
