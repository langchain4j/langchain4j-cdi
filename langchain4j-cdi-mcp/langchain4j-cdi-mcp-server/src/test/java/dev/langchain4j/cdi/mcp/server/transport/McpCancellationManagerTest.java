package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class McpCancellationManagerTest {

    @Test
    void shouldRegisterAndCancelRequest() {
        McpCancellationManager manager = new McpCancellationManager();
        AtomicBoolean flag = manager.register("req-1");

        assertThat(flag.get()).isFalse();

        manager.cancel("req-1");
        assertThat(flag.get()).isTrue();
    }

    @Test
    void shouldHandleCancelOfUnknownRequest() {
        McpCancellationManager manager = new McpCancellationManager();
        // Should not throw
        manager.cancel("unknown");
    }

    @Test
    void shouldUnregisterRequest() {
        McpCancellationManager manager = new McpCancellationManager();
        AtomicBoolean flag = manager.register("req-1");
        manager.unregister("req-1");

        // Cancel after unregister should not affect the flag
        manager.cancel("req-1");
        assertThat(flag.get()).isFalse();
    }
}
