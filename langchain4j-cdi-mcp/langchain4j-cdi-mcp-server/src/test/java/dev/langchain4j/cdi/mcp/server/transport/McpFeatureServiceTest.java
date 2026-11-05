package dev.langchain4j.cdi.mcp.server.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpInvalidArgumentException;
import dev.langchain4j.cdi.mcp.server.error.McpToolNotFoundException;
import dev.langchain4j.cdi.mcp.server.registry.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class McpFeatureServiceTest {

    McpToolRegistry toolRegistry;
    McpToolInvoker toolInvoker;
    McpResourceRegistry resourceRegistry;
    McpBeanInvoker beanInvoker;
    McpFeatureService service;

    @BeforeEach
    void setup() {
        toolRegistry = mock(McpToolRegistry.class);
        toolInvoker = mock(McpToolInvoker.class);
        resourceRegistry = mock(McpResourceRegistry.class);
        beanInvoker = mock(McpBeanInvoker.class);
        service = new McpFeatureService(
                toolRegistry,
                resourceRegistry,
                mock(McpPromptRegistry.class),
                toolInvoker,
                beanInvoker,
                new McpCancellationManager(),
                new McpServerConfigResolver(new McpServerConfig("srv", "1.2")));
    }

    @Test
    void listToolsReturnsJsonObjectWithToolsArray() {
        when(toolRegistry.listTools()).thenReturn(List.of());

        JsonObject result = service.listTools(null);

        assertThat(result.getJsonArray("tools")).isEmpty();
        assertThat(result.containsKey("nextCursor")).isFalse();
    }

    @Test
    void featureResultsCarryNoCachingHints() {
        // SEP-2549 ttlMs/cacheScope are added by McpModernProtocolHandler.cacheable(); the legacy era serialises
        // these results verbatim, so legacy list results must stay free of them
        when(toolRegistry.listTools()).thenReturn(List.of());

        assertThat(service.listTools(null)).doesNotContainKey("ttlMs").doesNotContainKey("cacheScope");
    }

    @Test
    void callUnknownToolFails() {
        when(toolRegistry.findTool("nope")).thenReturn(Optional.empty());
        JsonObject params = Json.createObjectBuilder().add("name", "nope").build();

        assertThatThrownBy(() -> service.callTool(1, params, ctx(), null)).isInstanceOf(McpToolNotFoundException.class);
    }

    @Test
    void callToolWrapsPlainResultAsTextContent() {
        McpToolDescriptor descriptor = mock(McpToolDescriptor.class);
        when(toolRegistry.findTool("greet")).thenReturn(Optional.of(descriptor));
        when(toolInvoker.invoke(eq(1), eq(descriptor), any(), any(), any())).thenReturn("Hello");
        JsonObject params = Json.createObjectBuilder().add("name", "greet").build();

        JsonObject result = service.callTool(1, params, ctx(), null);

        assertThat(result.getJsonArray("content").getJsonObject(0).getString("text"))
                .isEqualTo("Hello");
    }

    // --- langchain4j-cdi#298: an argument that cannot be bound, answered by era ---

    private McpToolDescriptor toolRejecting(String name) {
        McpToolDescriptor descriptor = mock(McpToolDescriptor.class);
        when(toolRegistry.findTool(name)).thenReturn(Optional.of(descriptor));
        when(toolInvoker.invoke(any(), eq(descriptor), any(), any(), any()))
                .thenThrow(new McpInvalidArgumentException(null, "limit", "expected integer, got string \"5\""));
        return descriptor;
    }

    @Test
    void anInvalidToolArgumentIsAToolExecutionErrorInTheModernEra() {
        toolRejecting("list_tasks");
        JsonObject params = Json.createObjectBuilder().add("name", "list_tasks").build();

        JsonObject result = service.callTool(23, params, modernCtx(), null);

        assertThat(result.getBoolean("isError")).isTrue();
        assertThat(result.getJsonArray("content").getJsonObject(0).getString("text"))
                .isEqualTo("Invalid argument 'limit': expected integer, got string \"5\"");
    }

    @Test
    void anInvalidToolArgumentIsInvalidParamsInTheLegacyEra() {
        toolRejecting("list_tasks");
        JsonObject params = Json.createObjectBuilder().add("name", "list_tasks").build();

        assertThatThrownBy(() -> service.callTool(7, params, ctx(), null))
                .isInstanceOfSatisfying(McpException.class, e -> {
                    assertThat(e.getErrorCode().getCode()).isEqualTo(-32602);
                    assertThat(e.getRequestId()).isEqualTo(7);
                    assertThat(e.getHttpStatus()).isEqualTo(200);
                    assertThat(e.getMessage())
                            .isEqualTo("Invalid argument 'limit': expected integer, got string \"5\"");
                });
    }

    // --- langchain4j-cdi#291: the request id is filled in, the failure behind the error is kept ---

    @Test
    void theRequestIdIsFilledInWithoutLosingTheCause() {
        McpToolDescriptor descriptor = mock(McpToolDescriptor.class);
        when(toolRegistry.findTool("fail")).thenReturn(Optional.of(descriptor));
        IllegalStateException root = new IllegalStateException("database unreachable");
        when(toolInvoker.invoke(any(), eq(descriptor), any(), any(), any()))
                .thenThrow(new McpException(null, McpErrorCode.INTERNAL_ERROR, "Tool execution failed", root));
        JsonObject params = Json.createObjectBuilder().add("name", "fail").build();

        assertThatThrownBy(() -> service.callTool(7, params, ctx(), null))
                .isInstanceOfSatisfying(McpException.class, e -> {
                    assertThat(e.getRequestId()).isEqualTo(7);
                    assertThat(e.getErrorCode()).isEqualTo(McpErrorCode.INTERNAL_ERROR);
                    assertThat(e.getMessage()).isEqualTo("Tool execution failed");
                    assertThat(e.getCause()).isSameAs(root);
                });
    }

    @Test
    void anInvalidPromptArgumentIsInvalidParamsInBothEras() throws Exception {
        McpPromptRegistry promptRegistry = new McpPromptRegistry();
        promptRegistry.register(new McpPromptDescriptor(
                "plan_day",
                "Plan my day",
                List.of(),
                TemplateBean.class,
                TemplateBean.class.getMethod("data", String.class)));
        McpFeatureService prompts = new McpFeatureService(
                toolRegistry,
                resourceRegistry,
                promptRegistry,
                toolInvoker,
                beanInvoker,
                new McpCancellationManager(),
                new McpServerConfigResolver(new McpServerConfig("srv", "1.2")));
        when(beanInvoker.invoke(any(), any(), any(), any(), any(), any()))
                .thenThrow(new McpInvalidArgumentException(null, "hours", "expected integer, got string \"six\""));
        JsonObject params = Json.createObjectBuilder().add("name", "plan_day").build();

        for (McpRequestContext context : List.of(ctx(), modernCtx())) {
            assertThatThrownBy(() -> prompts.getPrompt(9, params, context, null))
                    .isInstanceOfSatisfying(McpException.class, e -> {
                        assertThat(e.getErrorCode().getCode()).isEqualTo(-32602);
                        assertThat(e.getRequestId()).isEqualTo(9);
                    });
        }
    }

    @Test
    void serverInfoComesFromConfig() {
        assertThat(service.serverInfo().name()).isEqualTo("srv");
        assertThat(service.serverInfo().version()).isEqualTo("1.2");
        assertThat(service.capabilities().completions()).isNotNull();
    }

    // --- resources/read: exact URIs, URI templates, and the not-found error ---

    @SuppressWarnings("unused")
    static class TemplateBean {
        public String data(String id) {
            return id;
        }
    }

    private static JsonObject readRequest(String uri) {
        return Json.createObjectBuilder().add("uri", uri).build();
    }

    @Test
    void readResourceResolvesAUriTemplateAndBindsItsVariables() throws Exception {
        McpResourceTemplateDescriptor template = new McpResourceTemplateDescriptor(
                "test://template/{id}/data",
                "Template",
                "d",
                "application/json",
                TemplateBean.class,
                TemplateBean.class.getMethod("data", String.class));
        when(resourceRegistry.findResource("test://template/42/data")).thenReturn(Optional.empty());
        when(resourceRegistry.matchTemplate("test://template/42/data"))
                .thenReturn(Optional.of(new McpResourceRegistry.TemplateMatch(template, Map.of("id", "42"))));
        when(beanInvoker.invoke(eq(1), eq(TemplateBean.class), any(), any(), any(), any()))
                .thenReturn("{\"id\":\"42\"}");

        JsonObject result = service.readResource(1, readRequest("test://template/42/data"), ctx(), null);

        ArgumentCaptor<JsonObject> arguments = ArgumentCaptor.forClass(JsonObject.class);
        verify(beanInvoker).invoke(eq(1), eq(TemplateBean.class), any(), arguments.capture(), any(), any());
        assertThat(arguments.getValue().getString("id")).isEqualTo("42");

        JsonObject contents = result.getJsonArray("contents").getJsonObject(0);
        assertThat(contents.getString("uri")).isEqualTo("test://template/42/data");
        assertThat(contents.getString("mimeType")).isEqualTo("application/json");
        assertThat(contents.getString("text")).isEqualTo("{\"id\":\"42\"}");
    }

    @Test
    void readResourcePrefersAnExactUriOverATemplate() throws Exception {
        McpResourceDescriptor resource = mock(McpResourceDescriptor.class);
        when(resource.getBeanType()).thenAnswer(i -> TemplateBean.class);
        when(resource.getMethod()).thenReturn(TemplateBean.class.getMethod("data", String.class));
        when(resource.getMimeType()).thenReturn("text/plain");
        when(resourceRegistry.findResource("test://exact")).thenReturn(Optional.of(resource));
        when(beanInvoker.invoke(any(), any(), any(), any(), any(), any())).thenReturn("body");

        service.readResource(1, readRequest("test://exact"), ctx(), null);

        verify(resourceRegistry, never()).matchTemplate(any());
    }

    @Test
    void readResourceReportsTheRequestedUriInTheErrorData() {
        when(resourceRegistry.findResource("unknown://x")).thenReturn(Optional.empty());
        when(resourceRegistry.matchTemplate("unknown://x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.readResource(1, readRequest("unknown://x"), ctx(), null))
                .isInstanceOfSatisfying(McpException.class, e -> {
                    assertThat(e.getErrorCode().getCode()).isEqualTo(-32602);
                    assertThat(e.getMessage()).contains("unknown://x");
                    assertThat(e.getData()).isEqualTo(Map.of("uri", "unknown://x"));
                });
    }

    /**
     * Builds a feature service over a real {@link McpResourceRegistry} holding the given templates, so that URI
     * matching really runs instead of being stubbed.
     */
    private McpFeatureService withTemplates(String... uriTemplates) throws Exception {
        McpResourceRegistry realRegistry = new McpResourceRegistry();
        for (String uriTemplate : uriTemplates) {
            realRegistry.registerTemplate(new McpResourceTemplateDescriptor(
                    uriTemplate,
                    uriTemplate,
                    "d",
                    "application/json",
                    TemplateBean.class,
                    TemplateBean.class.getMethod("data", String.class)));
        }
        return new McpFeatureService(
                toolRegistry,
                realRegistry,
                mock(McpPromptRegistry.class),
                toolInvoker,
                beanInvoker,
                new McpCancellationManager(),
                new McpServerConfigResolver(new McpServerConfig("srv", "1.2")));
    }

    @Test
    void malformedPercentEncodingInAMatchingUriIsReadWithTheRawValue() throws Exception {
        McpFeatureService service = withTemplates("test://template/{id}/data");
        when(beanInvoker.invoke(any(), any(), any(), any(), any(), any())).thenReturn("body");

        JsonObject result = service.readResource(1, readRequest("test://template/100%/data"), ctx(), null);

        ArgumentCaptor<JsonObject> arguments = ArgumentCaptor.forClass(JsonObject.class);
        verify(beanInvoker).invoke(eq(1), eq(TemplateBean.class), any(), arguments.capture(), any(), any());
        assertThat(arguments.getValue().getString("id")).isEqualTo("100%");
        assertThat(result.getJsonArray("contents").getJsonObject(0).getString("uri"))
                .isEqualTo("test://template/100%/data");
    }

    @Test
    void illegalHexCharactersInAMatchingUriAreReadWithTheRawValue() throws Exception {
        McpFeatureService service = withTemplates("test://template/{id}/data");
        when(beanInvoker.invoke(any(), any(), any(), any(), any(), any())).thenReturn("body");

        service.readResource(1, readRequest("test://template/a%zzb/data"), ctx(), null);

        ArgumentCaptor<JsonObject> arguments = ArgumentCaptor.forClass(JsonObject.class);
        verify(beanInvoker).invoke(eq(1), eq(TemplateBean.class), any(), arguments.capture(), any(), any());
        assertThat(arguments.getValue().getString("id")).isEqualTo("a%zzb");
    }

    @Test
    void aMalformedUriMatchingNoTemplateIsAPlainResourceNotFound() throws Exception {
        McpFeatureService service = withTemplates("test://other/{id}");

        // never an IllegalArgumentException escaping to the container as a 500, never -32603
        assertThatThrownBy(() -> service.readResource(1, readRequest("test://template/100%/data"), ctx(), null))
                .isInstanceOfSatisfying(McpException.class, e -> {
                    assertThat(e.getErrorCode().getCode()).isEqualTo(-32602);
                    assertThat(e.getMessage()).contains("Resource not found");
                    assertThat(e.getData()).isEqualTo(Map.of("uri", "test://template/100%/data"));
                });
    }

    private static McpRequestContext ctx() {
        return new McpRequestContext(null, 1, null, new AtomicBoolean());
    }

    private static McpRequestContext modernCtx() {
        return new McpRequestContext(
                null,
                1,
                null,
                new AtomicBoolean(),
                new McpProtocolContext(McpEra.MODERN, "2026-07-28", JsonValue.EMPTY_JSON_OBJECT, null, null),
                null,
                null);
    }
}
