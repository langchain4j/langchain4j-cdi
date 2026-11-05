package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpToolNotFoundException;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcRequest;
import dev.langchain4j.cdi.mcp.server.registry.McpBeanInvoker;
import dev.langchain4j.cdi.mcp.server.registry.McpPromptRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpToolInvoker;
import dev.langchain4j.cdi.mcp.server.registry.McpToolRegistry;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A 2025-03-26 request whose parameters do not have the JSON types its method reads used to fail while the parameter
 * was read: {@code resources/read} with {@code {"uri": 42}} was answered {@code -32603 Internal error}, and
 * {@code tools/call} with {@code {"name": 42}} was answered {@code "Missing tool name"} — the value is there, it is
 * just not a string. The 2026-07-28 path checks the parameter types before dispatch and answers {@code -32602 Invalid
 * params} for both; the two eras read the same parameters, so both have to reject them the same way.
 *
 * <p>{@link McpFeatureService} is real here rather than a mock, because the failure being guarded against happens
 * inside it, while it reads {@code params}.
 */
class McpLegacyProtocolHandlerParamValidationTest {

    private McpLegacyProtocolHandler handler;
    private McpSessionManager sessionManager;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        sessionManager = new McpSessionManager();
        setField(sessionManager, "subscriptionManager", new McpResourceSubscriptionManager());
        setField(sessionManager, "rootsManager", new McpRootsManager());
        setField(sessionManager, "broadcaster", new McpNotificationBroadcaster());
        sessionId = sessionManager.createSession(null);

        handler = new McpLegacyProtocolHandler();
        handler.features = new McpFeatureService(
                mock(McpToolRegistry.class),
                mock(McpResourceRegistry.class),
                mock(McpPromptRegistry.class),
                mock(McpToolInvoker.class),
                mock(McpBeanInvoker.class),
                new McpCancellationManager(),
                new McpServerConfigResolver(new McpServerConfig("srv", "1.0")));
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
    void aToolsCallWithANonStringNameIsInvalidParams() {
        assertInvalidParams(request(7, "tools/call", Json.createObjectBuilder().add("name", 42)), "'name'");
    }

    @Test
    void aResourcesReadWithANonStringUriIsInvalidParams() {
        assertInvalidParams(
                request(8, "resources/read", Json.createObjectBuilder().add("uri", 42)), "'uri'");
    }

    @Test
    void aPromptsGetWithANonObjectArgumentsIsInvalidParams() {
        assertInvalidParams(
                request(
                        9,
                        "prompts/get",
                        Json.createObjectBuilder().add("name", "plan_day").add("arguments", "hours=6")),
                "'arguments'");
    }

    @Test
    void aCompletionWithANonObjectRefIsInvalidParams() {
        assertInvalidParams(
                request(10, "completion/complete", Json.createObjectBuilder().add("ref", "ref/prompt")), "'ref'");
    }

    @Test
    void aWellFormedToolsCallStillReachesTheToolRegistry() {
        // The registries are mocks, so an unknown tool is the proof that the request got past validation and was
        // dispatched: validation must not reject what the path is able to serve.
        assertThatThrownBy(() -> handler.handle(
                        request(11, "tools/call", Json.createObjectBuilder().add("name", "list_tasks")), sessionId))
                .isInstanceOf(McpToolNotFoundException.class);
    }

    private void assertInvalidParams(JsonRpcRequest request, String parameter) {
        assertThatThrownBy(() -> handler.handle(request, sessionId)).isInstanceOfSatisfying(McpException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(McpErrorCode.INVALID_PARAMS);
            assertThat(e.getErrorCode().getCode()).isEqualTo(-32602);
            assertThat(e.getRequestId()).isEqualTo(request.getId());
            assertThat(e.getMessage()).startsWith("Invalid params:").contains(parameter);
        });
    }

    private static JsonRpcRequest request(Object id, String method, JsonObjectBuilder params) {
        JsonObject built = params.build();
        return new JsonRpcRequest(id, method, built);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
