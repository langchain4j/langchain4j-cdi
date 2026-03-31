package dev.langchain4j.cdi.mcp.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonRpcResponseTest {

    @Test
    void shouldCreateSuccessResponse() {
        JsonRpcResponse response = JsonRpcResponse.success("1", "hello");

        assertThat(response.getJsonrpc()).isEqualTo("2.0");
        assertThat(response.getId()).isEqualTo("1");
        assertThat(response.getResult()).isEqualTo("hello");
        assertThat(response.getError()).isNull();
    }

    @Test
    void shouldCreateErrorResponse() {
        JsonRpcError error = new JsonRpcError(-32601, "Method not found");
        JsonRpcResponse response = JsonRpcResponse.error("2", error);

        assertThat(response.getJsonrpc()).isEqualTo("2.0");
        assertThat(response.getId()).isEqualTo("2");
        assertThat(response.getResult()).isNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo(-32601);
        assertThat(response.getError().getMessage()).isEqualTo("Method not found");
    }

    @Test
    void shouldPreserveNumericId() {
        JsonRpcResponse response = JsonRpcResponse.success(42L, "result");

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getId()).isInstanceOf(Long.class);
    }

    @Test
    void shouldPreserveNullId() {
        JsonRpcResponse response = JsonRpcResponse.success(null, "result");

        assertThat(response.getId()).isNull();
    }
}
