package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpInvalidArgumentException;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcRequest;
import jakarta.json.Json;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * langchain4j-cdi#298: on the 2025-03-26 path, an exception the handler did not foresee is answered as a JSON-RPC
 * {@code -32603 Internal error} (mapped by {@link McpExceptionMapper}) instead of reaching the Jakarta REST container,
 * whose error page is runtime-specific and may expose internal class names.
 */
class McpLegacyProtocolHandlerErrorTest {

    private McpLegacyProtocolHandler handler;
    private McpSessionManager sessionManager;
    private McpFeatureService features;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        sessionManager = new McpSessionManager();
        setField(sessionManager, "subscriptionManager", new McpResourceSubscriptionManager());
        setField(sessionManager, "rootsManager", new McpRootsManager());
        setField(sessionManager, "broadcaster", new McpNotificationBroadcaster());
        sessionId = sessionManager.createSession(null);

        features = mock(McpFeatureService.class);
        handler = new McpLegacyProtocolHandler();
        handler.features = features;
        handler.sessionManager = sessionManager;
        handler.broadcaster = new McpNotificationBroadcaster();
        handler.rootsManager = new McpRootsManager();
        handler.cancellationManager = new McpCancellationManager();
        handler.serverRequestManager = new McpServerRequestManager();
    }

    @AfterEach
    void tearDown() {
        sessionManager.shutdown();
    }

    @Test
    void anUnexpectedExceptionIsAnsweredAsInternalError() {
        when(features.callTool(eq(7), any(), any(), any()))
                .thenThrow(new ClassCastException(
                        "class org.eclipse.parsson.JsonStringImpl cannot be cast to class jakarta.json.JsonNumber"));

        assertThatThrownBy(() -> handler.handle(toolsCall(7), sessionId))
                .isInstanceOfSatisfying(McpException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(McpErrorCode.INTERNAL_ERROR);
                    assertThat(e.getMessage()).isEqualTo("Internal error");
                    assertThat(e.getRequestId()).isEqualTo(7);
                    assertThat(e.getHttpStatus()).isEqualTo(200);
                });
    }

    @Test
    void anInvalidArgumentPropagatesUnchangedAsInvalidParams() {
        McpInvalidArgumentException invalid =
                new McpInvalidArgumentException(7, "limit", "expected integer, got string \"5\"");
        when(features.callTool(eq(7), any(), any(), any())).thenThrow(invalid);

        assertThatThrownBy(() -> handler.handle(toolsCall(7), sessionId)).isSameAs(invalid);
    }

    private static JsonRpcRequest toolsCall(Object id) {
        return new JsonRpcRequest(
                id,
                "tools/call",
                Json.createObjectBuilder()
                        .add("name", "list_tasks")
                        .add("arguments", Json.createObjectBuilder().add("limit", "5"))
                        .build());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
