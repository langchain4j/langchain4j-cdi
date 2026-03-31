package dev.langchain4j.cdi.mcp.integrationtests.wildfly;

import static dev.langchain4j.cdi.mcp.integrationtests.McpTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.cdi.mcp.integrationtests.ConfigResource;
import dev.langchain4j.cdi.mcp.integrationtests.GreetingTool;
import dev.langchain4j.cdi.mcp.integrationtests.JsonRpcAssertions;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpResponse;
import dev.langchain4j.cdi.mcp.integrationtests.McpHttpTransport;
import dev.langchain4j.cdi.mcp.integrationtests.McpTestConstants;
import dev.langchain4j.cdi.mcp.integrationtests.McpTestRequests;
import dev.langchain4j.cdi.mcp.integrationtests.SummarizePrompt;
import dev.langchain4j.cdi.mcp.integrationtests.WeatherTool;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.service.tool.ToolExecutionResult;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly Arquillian integration tests. Arquillian requires public test methods and does not support test method
 * inheritance, so this class uses the shared helpers directly instead of extending {@code AbstractMcpIntegrationTest}.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "resource", "java:S5786"})
@ExtendWith(ArquillianExtension.class)
public class McpWildFlyArquillianTest {

    @SuppressWarnings("unused")
    @Deployment
    public static WebArchive createDeployment() throws IOException {
        File mcpServerFile = findBuildFiles(
                        new File("../../langchain4j-cdi-mcp-server/target").toPath(), "langchain4j-cdi-mcp-server-")
                .get()
                .toFile();
        File mcpPortableExtFile = findBuildFiles(
                        new File("../../langchain4j-cdi-mcp-portable-ext/target").toPath(),
                        "langchain4j-cdi-mcp-portable-ext-")
                .get()
                .toFile();

        File[] deps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .importRuntimeDependencies()
                .resolve(
                        "dev.langchain4j.cdi.mcp:langchain4j-cdi-mcp-portable-ext",
                        "dev.langchain4j:langchain4j-mcp",
                        "org.assertj:assertj-core",
                        "org.mcp-java:mcp-server-api")
                .withTransitivity()
                .asFile();

        File[] fixedDeps = Stream.concat(
                        Stream.of(mcpServerFile, mcpPortableExtFile),
                        Stream.of(deps).filter(f -> !f.getName().startsWith("langchain4j-cdi-mcp-")))
                .toArray(File[]::new);

        return ShrinkWrap.create(WebArchive.class, "mcp-test.war")
                .addClasses(
                        McpWildFlyArquillianTest.class,
                        WeatherTool.class,
                        GreetingTool.class,
                        ConfigResource.class,
                        SummarizePrompt.class,
                        JaxRsApplication.class,
                        JdkHttpClientTransport.class,
                        McpHttpTransport.class,
                        McpHttpResponse.class,
                        McpTestRequests.class,
                        McpTestConstants.class,
                        JsonRpcAssertions.class)
                .addAsLibraries(fixedDeps)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/services/jakarta.enterprise.inject.spi.Extension");
    }

    private static Optional<Path> findBuildFiles(Path folder, String prefix) throws IOException {
        return Files.find(
                        folder,
                        1,
                        (BiPredicate<Path, BasicFileAttributes>) (t, u) -> {
                            String fileName = t.getFileName().toString();
                            return fileName.startsWith(prefix) && fileName.endsWith(".jar");
                        },
                        FileVisitOption.FOLLOW_LINKS)
                .findFirst();
    }

    @SuppressWarnings("unused")
    @ArquillianResource
    private URL baseURL;

    private McpHttpTransport transport() {
        return new JdkHttpClientTransport(baseURL.toString());
    }

    // --- Tools via MCP Client ---

    @Test
    public void shouldListToolsViaMcpClient() throws Exception {
        try (McpClient client = buildClient()) {
            List<ToolSpecification> tools = client.listTools();
            assertThat(tools)
                    .hasSizeGreaterThanOrEqualTo(2)
                    .extracting(ToolSpecification::name)
                    .contains(GET_WEATHER, GREET);
        }
    }

    @Test
    public void shouldCallToolViaMcpClient() throws Exception {
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
    public void shouldCallToolWithOptionalParamOmitted() throws Exception {
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
    public void shouldCallToolWithOptionalParamProvided() throws Exception {
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
    public void shouldHaveOptionalParameterInToolSchema() throws Exception {
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
    public void shouldCallToolWithUnknownNameViaMcpClient() throws Exception {
        try (McpClient client = buildClient()) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("nonExistentTool")
                    .arguments("{}")
                    .build();
            assertThatThrownBy(() -> client.executeTool(request)).isInstanceOf(Exception.class);
        }
    }

    @Test
    public void shouldListToolsMultipleTimes() throws Exception {
        try (McpClient client = buildClient()) {
            List<ToolSpecification> tools1 = client.listTools();
            List<ToolSpecification> tools2 = client.listTools();
            assertThat(tools1).hasSizeGreaterThanOrEqualTo(2);
            assertThat(tools2).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    // --- Session & HTTP ---

    @Test
    public void shouldInitializeSessionSuccessfully() {
        String sessionId = initializeSession();
        assertThat(sessionId).isNotNull().isNotBlank();
    }

    @Test
    public void shouldSendInitializedNotification() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.initializedNotification());
        JsonRpcAssertions.assertNotificationAccepted(response);
    }

    @Test
    public void shouldHandlePing() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.pingRequest(99));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 99);
    }

    @Test
    public void shouldDeleteSession() {
        String sessionId = initializeSession();
        McpHttpResponse response = transport().delete("/mcp", Map.of(MCP_SESSION_ID, sessionId));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    public void shouldReturnCleanJsonForToolCall() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(
                sessionId,
                McpTestRequests.toolsCallRequest(10, GET_WEATHER, "{\"city\":\"Paris\",\"unit\":\"celsius\"}"));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 10);
        JsonArray content = result.getJsonArray("content");
        assertThat(content).isNotNull().isNotEmpty();

        JsonObject firstContent = content.getJsonObject(0);
        assertThat(firstContent.getString("text")).contains("Paris");
        assertThat(firstContent).doesNotContainKey("data").doesNotContainKey("mimeType");
    }

    @Test
    public void shouldReturnErrorForUnknownMethod() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.unknownMethodRequest("5", "unknown/method"));
        JsonRpcAssertions.assertJsonRpcError(response, "5", -32601, "Unknown method");
    }

    // --- Resources ---

    @Test
    public void shouldListResources() {
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
    public void shouldReadResource() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesReadRequest(21, CONFIG_APP));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 21);
        JsonArray contents = result.getJsonArray("contents");
        assertThat(contents).isNotNull().isNotEmpty();
        assertThat(contents.getJsonObject(0).getString("uri")).isEqualTo(CONFIG_APP);
        assertThat(contents.getJsonObject(0).getString("text")).contains("version");
    }

    @Test
    public void shouldReturnErrorForUnknownResource() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesReadRequest(22, "unknown://x"));
        JsonRpcAssertions.assertJsonRpcError(response, 22, -32602, "Resource not found");
    }

    // --- Prompts ---

    @Test
    public void shouldListPrompts() {
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
    public void shouldGetPrompt() {
        String sessionId = initializeSession();
        McpHttpResponse response =
                postMcp(sessionId, McpTestRequests.promptsGetRequest(31, SUMMARIZE, "{\"text\":\"Hello world\"}"));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 31);
        JsonArray messages = result.getJsonArray("messages");
        assertThat(messages).isNotNull().isNotEmpty();

        JsonObject firstMessage = messages.getJsonObject(0);
        assertThat(firstMessage).containsKey("content");
        assertThat(firstMessage.getJsonObject("content").getString("text")).contains("Hello world");
    }

    @Test
    public void shouldReturnErrorForUnknownPrompt() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.promptsGetRequest(32, "nonexistent"));
        JsonRpcAssertions.assertJsonRpcError(response, 32, -32602, "Prompt not found");
    }

    // --- Logging ---

    @Test
    public void shouldSetLogLevel() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.loggingSetLevelRequest(40, "warning"));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 40);
    }

    @Test
    public void shouldReturnErrorForInvalidLogLevel() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.loggingSetLevelRequest(41, "banana"));
        JsonRpcAssertions.assertJsonRpcError(response, 41, -32602, "Invalid log level");
    }

    // --- Resource Subscriptions ---

    @Test
    public void shouldSubscribeToResource() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesSubscribeRequest(60, CONFIG_APP));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 60);
    }

    @Test
    public void shouldUnsubscribeFromResource() {
        String sessionId = initializeSession();
        postMcp(sessionId, McpTestRequests.resourcesSubscribeRequest(61, CONFIG_APP));

        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesUnsubscribeRequest(62, CONFIG_APP));
        JsonRpcAssertions.assertJsonRpcSuccess(response, 62);
    }

    // --- Resource Templates ---

    @Test
    public void shouldListResourceTemplates() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.resourcesTemplatesListRequest(63));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 63);
        assertThat(result).containsKey("resourceTemplates");
    }

    // --- Completion ---

    @Test
    public void shouldReturnEmptyCompletionForUnknownRef() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(
                sessionId, McpTestRequests.completionCompleteRequest(64, "ref/prompt", "nonexistent", "text", ""));

        JsonObject result = JsonRpcAssertions.assertJsonRpcSuccess(response, 64);
        assertThat(result).containsKey("completion");
        assertThat(result.getJsonObject("completion").getJsonArray("values")).isNotNull();
    }

    // --- Notifications ---

    @Test
    public void shouldAcknowledgeCancellation() {
        String sessionId = initializeSession();
        McpHttpResponse response = postMcp(sessionId, McpTestRequests.cancelledNotification("abc", "timeout"));
        JsonRpcAssertions.assertNotificationAccepted(response);
    }

    @Test
    public void shouldAcceptClientJsonRpcResponse() {
        String sessionId = initializeSession();
        McpHttpResponse response =
                postMcp(sessionId, McpTestRequests.clientJsonRpcResponse("server-999", "{\"roots\":[]}"));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    // --- Capabilities ---

    @Test
    public void shouldDeclareAllCapabilities() {
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
    public void shouldRejectRequestWithInvalidSessionId() {
        McpHttpResponse response = postMcp("bogus-session-id-12345", McpTestRequests.toolsListRequest(70));
        JsonRpcAssertions.assertJsonRpcError(response, 70, -32001, "Invalid or missing Mcp-Session-Id");
    }

    // --- Helpers ---

    private String initializeSession() {
        McpHttpResponse response = transport().post("/mcp", McpTestRequests.initializeRequest(1), Map.of());
        JsonRpcAssertions.assertJsonRpcSuccess(response, 1);
        String sessionId = response.header(MCP_SESSION_ID);
        assertThat(sessionId).as(MCP_SESSION_ID + " header").isNotNull().isNotBlank();
        return sessionId;
    }

    private McpHttpResponse postMcp(String sessionId, String body) {
        return transport().post("/mcp", body, Map.of(MCP_SESSION_ID, sessionId));
    }

    private McpClient buildClient() {
        return DefaultMcpClient.builder()
                .transport(StreamableHttpMcpTransport.builder()
                        .url(baseURL + "mcp")
                        .build())
                .build();
    }
}
