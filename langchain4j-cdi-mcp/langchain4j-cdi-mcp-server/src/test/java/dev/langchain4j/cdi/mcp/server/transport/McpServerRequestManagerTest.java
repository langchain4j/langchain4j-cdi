package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpServerRequestManagerTest {

    McpServerRequestManager manager;
    McpNotificationBroadcaster broadcaster;

    @BeforeEach
    void setup() throws Exception {
        manager = new McpServerRequestManager();
        broadcaster = mock(McpNotificationBroadcaster.class);
        Field field = McpServerRequestManager.class.getDeclaredField("broadcaster");
        field.setAccessible(true);
        field.set(manager, broadcaster);
    }

    @Test
    void shouldSendRequestAndReceiveResponse() throws Exception {
        // Simulate: when broadcaster sends, we immediately complete the response
        doAnswer(invocation -> {
                    // Extract the request id from the sent object and complete the pending future
                    // The first pending request will have id "server-1"
                    JsonObject result = Json.createObjectBuilder()
                            .add("roots", Json.createArrayBuilder())
                            .build();
                    manager.handleResponse("server-1", result);
                    return null;
                })
                .when(broadcaster)
                .sendToSession(eq("session-1"), any());

        JsonObject result = manager.sendRequest("session-1", "roots/list", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.containsKey("roots")).isTrue();
        verify(broadcaster).sendToSession(eq("session-1"), any());
    }

    @Test
    void shouldReturnNullOnTimeout() {
        // Don't complete the future, so it will timeout
        JsonObject result = manager.sendRequest("session-1", "roots/list", null, 1);

        assertThat(result).isNull();
    }

    @Test
    void shouldHandleErrorResponse() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        doAnswer(invocation -> {
                    // Simulate client error response
                    manager.handleErrorResponse("server-1", "Not supported");
                    return null;
                })
                .when(broadcaster)
                .sendToSession(eq("session-1"), any());

        JsonObject result = manager.sendRequest("session-1", "sampling/createMessage", null, 5);

        assertThat(result).isNull();
        executor.shutdown();
    }

    @Test
    void shouldTrackPendingRequests() {
        assertThat(manager.pendingRequestCount()).isZero();
    }
}
