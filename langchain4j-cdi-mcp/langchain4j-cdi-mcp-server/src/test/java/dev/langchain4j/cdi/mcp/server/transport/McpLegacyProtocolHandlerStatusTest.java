package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpSessionException;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcRequest;
import jakarta.json.JsonValue;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Session-related Streamable HTTP status codes of the legacy (2025-03-26) era: a request bearing an unknown or
 * terminated session id is answered {@code 404 Not Found}, one bearing no session id at all {@code 400 Bad Request}.
 *
 * <p>The {@code 202 Accepted} answer to a notification-only POST is asserted by the integration tests: building a
 * {@code jakarta.ws.rs.core.Response} needs a JAX-RS runtime, which this module does not have on its test classpath.
 */
class McpLegacyProtocolHandlerStatusTest {

    private McpLegacyProtocolHandler handler;
    private McpSessionManager sessionManager;

    @BeforeEach
    void setUp() throws Exception {
        sessionManager = new McpSessionManager();
        setField(sessionManager, "subscriptionManager", new McpResourceSubscriptionManager());
        setField(sessionManager, "rootsManager", new McpRootsManager());
        setField(sessionManager, "broadcaster", new McpNotificationBroadcaster());

        handler = new McpLegacyProtocolHandler();
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
    void terminatedSessionIsNotFound() {
        String sessionId = sessionManager.createSession(null);
        sessionManager.terminateSession(sessionId);

        assertThatThrownBy(() -> handler.handle(request(1, "tools/list"), sessionId))
                .isInstanceOfSatisfying(
                        McpSessionException.class,
                        e -> assertThat(e.getHttpStatus()).isEqualTo(404));
    }

    @Test
    void unknownSessionIsNotFound() {
        assertThatThrownBy(() -> handler.handle(request(1, "tools/list"), "bogus-session"))
                .isInstanceOfSatisfying(
                        McpSessionException.class,
                        e -> assertThat(e.getHttpStatus()).isEqualTo(404));
    }

    @Test
    void missingSessionIsBadRequest() {
        assertThatThrownBy(() -> handler.handle(request(1, "tools/list"), null))
                .isInstanceOfSatisfying(
                        McpSessionException.class,
                        e -> assertThat(e.getHttpStatus()).isEqualTo(400));
    }

    @Test
    void sessionErrorCodeStaysSessionNotFound() {
        assertThatThrownBy(() -> handler.handle(request(1, "tools/list"), "bogus-session"))
                .isInstanceOfSatisfying(
                        McpSessionException.class,
                        e -> assertThat(e.getErrorCode().getCode()).isEqualTo(-32001));
    }

    @Test
    void idLessInitializeIsRejectedAsInvalidRequest() throws Exception {
        assertThatThrownBy(() -> handler.handle(request(null, "initialize"), null))
                .isInstanceOfSatisfying(McpException.class, e -> {
                    assertThat(e.getErrorCode().getCode()).isEqualTo(-32600);
                    assertThat(e.getRequestId()).isNull();
                });

        assertThat(sessionCount())
                .as("no session should have been created for an id-less initialize")
                .isZero();
    }

    @SuppressWarnings("unchecked")
    private int sessionCount() throws Exception {
        Field field = McpSessionManager.class.getDeclaredField("sessions");
        field.setAccessible(true);
        return ((Map<String, ?>) field.get(sessionManager)).size();
    }

    private static JsonRpcRequest request(Object id, String method) {
        return new JsonRpcRequest(id, method, JsonValue.EMPTY_JSON_OBJECT);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
