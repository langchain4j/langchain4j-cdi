package dev.langchain4j.cdi.mcp.integrationtests;

import static dev.langchain4j.cdi.mcp.integrationtests.McpTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.service.tool.ToolExecutionResult;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S112")
public abstract class AbstractMcpIntegrationTest {

    public static final String CONTENT = "content";

    protected abstract McpHttpTransport transport();

    // --- Tools via MCP Client ---

    @Test
    void shouldListToolsViaMcpClient() throws Exception {
        try (McpClient client = buildClient()) {
            List<ToolSpecification> tools = client.listTools();
            assertThat(tools)
                    .hasSizeGreaterThanOrEqualTo(2)
                    .extracting(ToolSpecification::name)
                    .contains(GET_WEATHER, GREET);
        }
    }

    @Test
    void shouldCallToolViaMcpClient() throws Exception {
        try (McpClient client = buildClient()) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name(GET_WEATHER)
                    .arguments("{\"city\":\"Paris\",\"unit\":\"celsius\"}")
                    .build();
            ToolExecutionResult result = client.executeTool(request);
            assertThat(result.resultText()).contains("Paris");
        }
    }

    @Test
    void shouldCallToolWithOptionalParamOmitted() throws Exception {
        try (McpClient client = buildClient()) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name(GREET)
                    .arguments("{\"name\":\"Alice\"}")
                    .build();
            ToolExecutionResult result = client.executeTool(request);
            assertThat(result.resultText()).isEqualTo("Hello, Alice!");
        }
    }

    @Test
    void shouldCallToolWithOptionalParamProvided() throws Exception {
        try (McpClient client = buildClient()) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name(GREET)
                    .arguments("{\"name\":\"Bob\",\"prefix\":\"Bonjour\"}")
                    .build();
            ToolExecutionResult result = client.executeTool(request);
            assertThat(result.resultText()).isEqualTo("Bonjour, Bob!");
        }
    }

    @Test
    void shouldHaveOptionalParameterInToolSchema() throws Exception {
        try (McpClient client = buildClient()) {
            List<ToolSpecification> tools = client.listTools();
            ToolSpecification greet = tools.stream()
                    .filter(t -> t.name().equals(GREET))
                    .findFirst()
                    .orElseThrow();

            assertThat(greet.description()).isEqualTo("Greet someone by name");
            assertThat(greet.parameters().required()).contains("name").doesNotContain("prefix");
        }
    }

    @Test
    void shouldCallToolWithUnknownNameViaMcpClient() throws Exception {
        try (McpClient client = buildClient()) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("nonExistentTool")
                    .arguments("{}")
                    .build();
            assertThatThrownBy(() -> client.executeTool(request)).isInstanceOf(Exception.class);
        }
    }

    @Test
    void shouldListToolsMultipleTimes() throws Exception {
        try (McpClient client = buildClient()) {
            List<ToolSpecification> tools1 = client.listTools();
            List<ToolSpecification> tools2 = client.listTools();
            assertThat(tools1).hasSizeGreaterThanOrEqualTo(2);
            assertThat(tools2).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    // --- Session & HTTP ---

    @Test
    void shouldInitializeSessionSuccessfully() {
        String sessionId = initializeSession();
        assertThat(sessionId).isNotNull().isNotBlank();
    }

    @Test
    void shouldSendInitializedNotification() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.initializedNotification());
        JsonRpcAssertions.assertNotificationAccepted(response);
    }

    @Test
    void shouldHandlePing() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.pingRequest(99));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 99);
    }

    @Test
    void shouldDeleteSession() {
        String sessionId = initializeSession();
        McpHttpResponse response = transport().delete("/mcp", Map.of(MCP_SESSION_ID, sessionId));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldReturnCleanJsonForToolCall() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(
                sessionId,
                McpTestRequests.toolsCallRequest(10, GET_WEATHER, "{\"city\":\"Paris\",\"unit\":\"celsius\"}"));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 10);
        JsonArray content = result.getJsonArray(CONTENT);
        assertThat(content).isNotNull().isNotEmpty();

        JsonObject firstContent = content.getJsonObject(0);
        assertThat(firstContent.getString("text")).contains("Paris");
        assertThat(firstContent).doesNotContainKey("data").doesNotContainKey("mimeType");
    }

    @Test
    void shouldReturnErrorForUnknownMethod() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.unknownMethodRequest("5", "unknown/method"));
        JsonRpcAssertions.assertJsonRpcError(response, "5", -32601, "Unknown method");
    }

    // --- Resources ---

    @Test
    void shouldListResources() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesListRequest(20));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 20);
        JsonArray resources = result.getJsonArray(RESOURCES);
        assertThat(resources).isNotNull();

        List<String> uris =
                resources.stream().map(v -> v.asJsonObject().getString("uri")).toList();
        assertThat(uris).contains(CONFIG_APP, "data://status");

        List<String> names =
                resources.stream().map(v -> v.asJsonObject().getString("name")).toList();
        assertThat(names).contains("Application Config");
    }

    @Test
    void shouldReadResource() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesReadRequest(21, CONFIG_APP));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 21);
        JsonArray contents = result.getJsonArray("contents");
        assertThat(contents).isNotNull().isNotEmpty();
        assertThat(contents.getJsonObject(0).getString("uri")).isEqualTo(CONFIG_APP);
        assertThat(contents.getJsonObject(0).getString("text")).contains("version");
    }

    @Test
    void shouldReturnErrorForUnknownResource() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesReadRequest(22, "unknown://x"));
        JsonRpcAssertions.assertJsonRpcError(response, 22, -32602, "Resource not found");
    }

    // --- Prompts ---

    @Test
    void shouldListPrompts() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.promptsListRequest(30));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 30);
        JsonArray prompts = result.getJsonArray("prompts");
        assertThat(prompts).isNotNull();

        List<String> names =
                prompts.stream().map(v -> v.asJsonObject().getString("name")).toList();
        assertThat(names).contains(SUMMARIZE);
    }

    @Test
    void shouldGetPrompt() {
        String sessionId = initializeSession();
        McpHttpResponse response =
                postMcp(sessionId, McpTestRequests.promptsGetRequest(31, SUMMARIZE, "{\"text\":\"Hello world\"}"));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 31);
        JsonArray messages = result.getJsonArray("messages");
        assertThat(messages).isNotNull().isNotEmpty();

        JsonObject firstMessage = messages.getJsonObject(0);
        assertThat(firstMessage).containsKey(CONTENT);
        assertThat(firstMessage.getJsonObject(CONTENT).getString("text")).contains("Hello world");
    }

    @Test
    void shouldReturnErrorForUnknownPrompt() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.promptsGetRequest(32, "nonexistent"));
        JsonRpcAssertions.assertJsonRpcError(response, 32, -32602, "Prompt not found");
    }

    // --- Logging ---

    @Test
    void shouldSetLogLevel() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.loggingSetLevelRequest(40, "warning"));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 40);
    }

    @Test
    void shouldReturnErrorForInvalidLogLevel() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.loggingSetLevelRequest(41, "banana"));
        JsonRpcAssertions.assertJsonRpcError(response, 41, -32602, "Invalid log level");
    }

    // --- Resource Subscriptions ---

    @Test
    void shouldSubscribeToResource() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesSubscribeRequest(60, CONFIG_APP));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 60);
    }

    @Test
    void shouldUnsubscribeFromResource() {
        String sessionId = initializeSession();
        postMcp(sessionId, McpTestRequests.resourcesSubscribeRequest(61, CONFIG_APP));

        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesUnsubscribeRequest(62, CONFIG_APP));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 62);
    }

    // --- Resource Templates ---

    @Test
    void shouldListResourceTemplates() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesTemplatesListRequest(63));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 63);
        assertThat(result).containsKey("resourceTemplates");
    }

    // --- Completion ---

    @Test
    void shouldReturnEmptyCompletionForUnknownRef() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(
                sessionId, McpTestRequests.completionCompleteRequest(64, "ref/prompt", "nonexistent", "text", ""));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 64);
        assertThat(result).containsKey("completion");
        assertThat(result.getJsonObject("completion").getJsonArray("values")).isNotNull();
    }

    // --- Notifications ---

    @Test
    void shouldAcknowledgeCancellation() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.cancelledNotification("abc", "timeout"));
        JsonRpcAssertions.assertNotificationAccepted(response);
    }

    @Test
    void shouldAcceptClientJsonRpcResponse() {
        String sessionId = initializeSession();
        McpHttpResponse response =
                postMcp(sessionId, McpTestRequests.clientJsonRpcResponse("server-999", "{\"roots\":[]}"));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    // --- Capabilities ---

    @Test
    void shouldDeclareAllCapabilities() {
        McpHttpResponse response = transport().post("/mcp", McpTestRequests.initializeRequest(50), Map.of());

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 50);
        assertThat(result).containsKey("capabilities");

        JsonObject capabilities = result.getJsonObject("capabilities");
        assertThat(capabilities)
                .containsKey("tools")
                .containsKey(RESOURCES)
                .containsKey("prompts")
                .containsKey("logging");

        JsonObject resourcesCap = capabilities.getJsonObject(RESOURCES);
        assertThat(resourcesCap).containsKey("subscribe");
    }

    // --- Negative session tests ---

    @Test
    void shouldRejectRequestWithInvalidSessionId() {
        McpHttpResponse response = postMcp("bogus-session-id-12345", McpTestRequests.toolsListRequest(70));
        JsonRpcAssertions.assertJsonRpcError(response, 70, -32001, "Invalid or missing Mcp-Session-Id");
    }

    // --- Helpers ---

    protected String initializeSession() {
        McpHttpResponse response = transport().post("/mcp", McpTestRequests.initializeRequest(1), Map.of());
        JsonRpcAssertions.assertJsonRpcSuccess(response, 1);
        String sessionId = response.header(MCP_SESSION_ID);
        assertThat(sessionId).as(MCP_SESSION_ID + " header").isNotNull().isNotBlank();
        return sessionId;
    }

    protected McpHttpResponse postMcp(String sessionId, String body) {
        return transport().post("/mcp", body, Map.of(MCP_SESSION_ID, sessionId));
    }

    protected McpClient buildClient() {
        return DefaultMcpClient.builder()
                .transport(StreamableHttpMcpTransport.builder()
                        .url(transport().baseUrl() + "/mcp")
                        .build())
                .build();
    }
}
