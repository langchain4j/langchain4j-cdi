package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpToolNotFoundException;
import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcRequest;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcResponse;
import dev.langchain4j.cdi.mcp.server.protocol.McpPagination;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptGetResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptMessage;
import dev.langchain4j.cdi.mcp.server.registry.McpBeanInvoker;
import dev.langchain4j.cdi.mcp.server.registry.McpPromptDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpPromptRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceTemplateDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpToolDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpToolInvoker;
import dev.langchain4j.cdi.mcp.server.registry.McpToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mcp_java.model.common.Cursor;
import org.mcp_java.model.completion.CompleteResult;
import org.mcp_java.model.content.TextContent;
import org.mcp_java.model.lifecycle.Implementation;
import org.mcp_java.model.lifecycle.InitializeResult;
import org.mcp_java.model.lifecycle.ServerCapabilities;
import org.mcp_java.model.prompt.ListPromptsResult;
import org.mcp_java.model.prompt.PromptArgument;
import org.mcp_java.model.resource.ListResourceTemplatesResult;
import org.mcp_java.model.resource.ListResourcesResult;
import org.mcp_java.model.resource.ReadResourceResult;
import org.mcp_java.model.resource.ResourceContents;
import org.mcp_java.model.tool.CallToolResult;
import org.mcp_java.model.tool.ListToolsResult;

@Path("/mcp")
@ApplicationScoped
@SuppressWarnings("java:S1192")
public class McpEndpoint {

    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_NO_CACHE = "no-cache";
    private static final String HEADER_SESSION_ID = "Mcp-Session-Id";
    private static final String FIELD_METHOD = "method";
    private static final String FIELD_RESULT = "result";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_ARGUMENTS = "arguments";
    private static final String FIELD_PROGRESS_TOKEN = "progressToken";

    private McpToolRegistry toolRegistry;
    private McpResourceRegistry resourceRegistry;
    private McpPromptRegistry promptRegistry;
    private McpSessionManager sessionManager;
    private McpToolInvoker toolInvoker;
    private McpBeanInvoker beanInvoker;
    private McpNotificationBroadcaster broadcaster;
    private McpLogger mcpLogger;
    private McpResourceSubscriptionManager subscriptionManager;
    private McpServerRequestManager serverRequestManager;
    private McpRootsManager rootsManager;
    private McpCancellationManager cancellationManager;
    private Instance<McpServerConfig> configInstance;

    /** No-arg constructor required by CDI proxying and JAX-RS runtimes. */
    public McpEndpoint() {}

    @Inject
    public McpEndpoint(
            McpToolRegistry toolRegistry,
            McpResourceRegistry resourceRegistry,
            McpPromptRegistry promptRegistry,
            McpSessionManager sessionManager,
            McpToolInvoker toolInvoker,
            McpBeanInvoker beanInvoker,
            McpNotificationBroadcaster broadcaster,
            McpLogger mcpLogger,
            McpResourceSubscriptionManager subscriptionManager,
            McpServerRequestManager serverRequestManager,
            McpRootsManager rootsManager,
            McpCancellationManager cancellationManager,
            @Named("mcp-server") Instance<McpServerConfig> configInstance) {
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.promptRegistry = promptRegistry;
        this.sessionManager = sessionManager;
        this.toolInvoker = toolInvoker;
        this.beanInvoker = beanInvoker;
        this.broadcaster = broadcaster;
        this.mcpLogger = mcpLogger;
        this.subscriptionManager = subscriptionManager;
        this.serverRequestManager = serverRequestManager;
        this.rootsManager = rootsManager;
        this.cancellationManager = cancellationManager;
        this.configInstance = configInstance;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.SERVER_SENT_EVENTS})
    public Response handlePost(
            String body, @HeaderParam("Mcp-Session-Id") String sessionId, @HeaderParam("Accept") String accept) {

        // Check if this is a JSON-RPC response (from client, in reply to a server-initiated request)
        if (isJsonRpcResponse(body)) {
            return handleClientResponse(body);
        }

        JsonRpcRequest request = parseRequest(body);

        if (request.getMethod() == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_REQUEST, "Missing method");
        }

        boolean wantsSse = accept != null && accept.contains("text/event-stream");

        return switch (request.getMethod()) {
            case "initialize" -> handleInitialize(request, wantsSse);
            case "notifications/initialized" -> handleInitialized(request, sessionId);
            case "tools/list" -> handleToolsList(request, sessionId, wantsSse);
            case "tools/call" -> handleToolsCall(request, sessionId, wantsSse);
            case "resources/list" -> handleResourcesList(request, sessionId, wantsSse);
            case "resources/read" -> handleResourcesRead(request, sessionId, wantsSse);
            case "resources/subscribe" -> handleResourcesSubscribe(request, sessionId);
            case "resources/unsubscribe" -> handleResourcesUnsubscribe(request, sessionId);
            case "resources/templates/list" -> handleResourcesTemplatesList(request, sessionId, wantsSse);
            case "prompts/list" -> handlePromptsList(request, sessionId, wantsSse);
            case "prompts/get" -> handlePromptsGet(request, sessionId, wantsSse);
            case "completion/complete" -> handleCompletionComplete(request, sessionId, wantsSse);
            case "logging/setLevel" -> handleLoggingSetLevel(request, sessionId);
            case "ping" -> handlePing(request);
            case "notifications/cancelled" -> handleNotificationsCancelled(request);
            case "notifications/roots/list_changed" -> handleRootsListChanged(request, sessionId);
            default ->
                throw new McpException(
                        request.getId(), McpErrorCode.METHOD_NOT_FOUND, "Unknown method: " + request.getMethod());
        };
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Response handleGet(@HeaderParam("Mcp-Session-Id") String sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        sessionManager.requireSession(null, sessionId);
        StreamingOutput stream = out -> {
            broadcaster.registerStream(sessionId, out);
            try {
                out.write(": stream opened\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                broadcaster.unregisterStream(sessionId);
            }
        };
        return Response.ok(stream, MediaType.SERVER_SENT_EVENTS)
                .header(HEADER_CACHE_CONTROL, HEADER_NO_CACHE)
                .header(HEADER_SESSION_ID, sessionId)
                .build();
    }

    @DELETE
    public Response handleDelete(@HeaderParam("Mcp-Session-Id") String sessionId) {
        if (sessionId != null) {
            sessionManager.terminateSession(sessionId);
        }
        return Response.ok().build();
    }

    private Response handleInitialize(JsonRpcRequest request, boolean wantsSse) {
        String newSessionId = sessionManager.createSession(request.getParams());
        McpServerConfig config = resolveConfig();

        InitializeResult result = InitializeResult.of(
                "2025-03-26",
                new ServerCapabilities(
                        new ServerCapabilities.ToolsCapability(true),
                        new ServerCapabilities.ResourcesCapability(true, true),
                        new ServerCapabilities.PromptsCapability(true),
                        new ServerCapabilities.LoggingCapability()),
                Implementation.of(config.getServerName(), config.getServerVersion()));

        if (wantsSse) {
            String json = serializeToJson(JsonRpcResponse.success(request.getId(), result));
            String payload = "event: message\ndata: " + json + "\n\n";
            StreamingOutput stream = out -> {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.flush();
            };
            return Response.ok(stream, MediaType.SERVER_SENT_EVENTS)
                    .header(HEADER_CACHE_CONTROL, HEADER_NO_CACHE)
                    .header(HEADER_SESSION_ID, newSessionId)
                    .build();
        }

        return Response.ok(serializeToJson(JsonRpcResponse.success(request.getId(), result)))
                .type(MediaType.APPLICATION_JSON)
                .header(HEADER_SESSION_ID, newSessionId)
                .build();
    }

    private Response handleInitialized(JsonRpcRequest request, String sessionId) {
        McpSession session = sessionManager.requireSession(request.getId(), sessionId);
        session.markInitialized();
        return Response.ok().build();
    }

    // --- Tools ---

    private Response handleToolsList(JsonRpcRequest request, String sessionId, boolean sse) {
        sessionManager.requireSession(request.getId(), sessionId);

        String cursor = extractCursor(request.getParams());
        List<McpToolDescriptor> allTools = new java.util.ArrayList<>(toolRegistry.listTools());
        McpPagination.Page<McpToolDescriptor> page = McpPagination.paginate(allTools, cursor);

        Cursor nextCursor = page.nextCursor() != null ? new Cursor(page.nextCursor()) : null;
        ListToolsResult result = new ListToolsResult(
                page.items().stream().map(McpToolDescriptor::toWireFormat).toList(), nextCursor);

        return respond(request.getId(), result, sse);
    }

    private Response handleToolsCall(JsonRpcRequest request, String sessionId, boolean sse) {
        McpSession session = sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        String toolName =
                params != null && params.containsKey("name") ? ((JsonString) params.get("name")).getString() : null;
        JsonObject arguments =
                params != null && params.containsKey(FIELD_ARGUMENTS) ? params.getJsonObject(FIELD_ARGUMENTS) : null;

        if (toolName == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing tool name");
        }

        McpToolDescriptor tool = toolRegistry
                .findTool(toolName)
                .orElseThrow(() -> new McpToolNotFoundException(request.getId(), toolName));

        AtomicBoolean cancelledFlag = cancellationManager.register(request.getId());
        McpRequestContext ctx =
                new McpRequestContext(sessionId, request.getId(), request.getProgressToken(), cancelledFlag);

        try {
            Object callResult = toolInvoker.invoke(request.getId(), tool, arguments, ctx, session);
            CallToolResult result;
            if (callResult == null) {
                result = CallToolResult.success(List.of());
            } else {
                result = CallToolResult.success(List.of(TextContent.of(callResult.toString())));
            }
            return respond(request.getId(), result, sse);
        } catch (McpException e) {
            throw new McpException(request.getId(), e.getErrorCode(), e.getMessage());
        } finally {
            cancellationManager.unregister(request.getId());
        }
    }

    // --- Resources ---

    private Response handleResourcesList(JsonRpcRequest request, String sessionId, boolean sse) {
        sessionManager.requireSession(request.getId(), sessionId);

        String cursor = extractCursor(request.getParams());
        List<McpResourceDescriptor> allResources = new java.util.ArrayList<>(resourceRegistry.listResources());
        McpPagination.Page<McpResourceDescriptor> page = McpPagination.paginate(allResources, cursor);

        Cursor nextCursor = page.nextCursor() != null ? new Cursor(page.nextCursor()) : null;
        ListResourcesResult result = new ListResourcesResult(
                page.items().stream()
                        .map(r -> org.mcp_java.model.resource.Resource.of(
                                r.getUri(), r.getName(), r.getDescription(), r.getMimeType()))
                        .toList(),
                nextCursor);

        return respond(request.getId(), result, sse);
    }

    private Response handleResourcesRead(JsonRpcRequest request, String sessionId, boolean sse) {
        McpSession session = sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        String uri = params != null && params.containsKey("uri") ? params.getString("uri") : null;

        if (uri == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing resource URI");
        }

        McpResourceDescriptor resource = resourceRegistry
                .findResource(uri)
                .orElseThrow(() ->
                        new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Resource not found: " + uri));

        McpRequestContext ctx =
                new McpRequestContext(sessionId, request.getId(), request.getProgressToken(), new AtomicBoolean(false));

        try {
            Object content = beanInvoker.invoke(
                    request.getId(), resource.getBeanType(), resource.getMethod(), null, ctx, session);
            String text = content != null ? content.toString() : "";
            ReadResourceResult result =
                    ReadResourceResult.of(List.of(ResourceContents.text(uri, resource.getMimeType(), text)));
            return respond(request.getId(), result, sse);
        } catch (McpException e) {
            throw new McpException(request.getId(), e.getErrorCode(), e.getMessage());
        }
    }

    // --- Prompts ---

    private Response handlePromptsList(JsonRpcRequest request, String sessionId, boolean sse) {
        sessionManager.requireSession(request.getId(), sessionId);

        String cursor = extractCursor(request.getParams());
        List<McpPromptDescriptor> allPrompts = new java.util.ArrayList<>(promptRegistry.listPrompts());
        McpPagination.Page<McpPromptDescriptor> page = McpPagination.paginate(allPrompts, cursor);

        Cursor nextCursor = page.nextCursor() != null ? new Cursor(page.nextCursor()) : null;
        ListPromptsResult result = new ListPromptsResult(
                page.items().stream()
                        .map(p -> org.mcp_java.model.prompt.Prompt.of(
                                p.getName(),
                                p.getDescription(),
                                p.getArguments().stream()
                                        .map(a -> new PromptArgument(a.name(), a.description(), a.required()))
                                        .toList()))
                        .toList(),
                nextCursor);

        return respond(request.getId(), result, sse);
    }

    private Response handlePromptsGet(JsonRpcRequest request, String sessionId, boolean sse) {
        McpSession session = sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        String promptName = params != null && params.containsKey("name") ? params.getString("name") : null;

        if (promptName == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing prompt name");
        }

        JsonObject arguments = params.containsKey(FIELD_ARGUMENTS) ? params.getJsonObject(FIELD_ARGUMENTS) : null;

        McpPromptDescriptor prompt = promptRegistry
                .findPrompt(promptName)
                .orElseThrow(() -> new McpException(
                        request.getId(), McpErrorCode.INVALID_PARAMS, "Prompt not found: " + promptName));

        McpRequestContext ctx =
                new McpRequestContext(sessionId, request.getId(), request.getProgressToken(), new AtomicBoolean(false));

        try {
            Object callResult = beanInvoker.invoke(
                    request.getId(), prompt.getBeanType(), prompt.getMethod(), arguments, ctx, session);
            McpPromptGetResult result;
            if (callResult instanceof List<?> messages) {
                @SuppressWarnings("unchecked")
                List<McpPromptMessage> typedMessages = (List<McpPromptMessage>) messages;
                result = new McpPromptGetResult(prompt.getDescription(), typedMessages);
            } else {
                String text = callResult != null ? callResult.toString() : "";
                result = new McpPromptGetResult(prompt.getDescription(), List.of(McpPromptMessage.user(text)));
            }
            return respond(request.getId(), result, sse);
        } catch (McpException e) {
            throw new McpException(request.getId(), e.getErrorCode(), e.getMessage());
        }
    }

    // --- Resource Subscriptions ---

    private Response handleResourcesSubscribe(JsonRpcRequest request, String sessionId) {
        sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        String uri = params != null && params.containsKey("uri") ? params.getString("uri") : null;

        if (uri == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing resource URI");
        }

        subscriptionManager.subscribe(sessionId, uri);

        return Response.ok(serializeToJson(JsonRpcResponse.success(request.getId(), Map.of())))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleResourcesUnsubscribe(JsonRpcRequest request, String sessionId) {
        sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        String uri = params != null && params.containsKey("uri") ? params.getString("uri") : null;

        if (uri == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing resource URI");
        }

        subscriptionManager.unsubscribe(sessionId, uri);

        return Response.ok(serializeToJson(JsonRpcResponse.success(request.getId(), Map.of())))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // --- Resource Templates ---

    private Response handleResourcesTemplatesList(JsonRpcRequest request, String sessionId, boolean sse) {
        sessionManager.requireSession(request.getId(), sessionId);

        String cursor = extractCursor(request.getParams());
        List<McpResourceTemplateDescriptor> allTemplates = new java.util.ArrayList<>(resourceRegistry.listTemplates());
        McpPagination.Page<McpResourceTemplateDescriptor> page = McpPagination.paginate(allTemplates, cursor);

        Cursor nextCursor = page.nextCursor() != null ? new Cursor(page.nextCursor()) : null;
        ListResourceTemplatesResult result = new ListResourceTemplatesResult(
                page.items().stream()
                        .map(t -> org.mcp_java.model.resource.ResourceTemplate.of(
                                t.getUriTemplate(), t.getName(), t.getDescription(), t.getMimeType()))
                        .toList(),
                nextCursor);

        return respond(request.getId(), result, sse);
    }

    // --- Completion ---

    private Response handleCompletionComplete(JsonRpcRequest request, String sessionId, boolean sse) {
        sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        JsonObject ref = params != null && params.containsKey("ref") ? params.getJsonObject("ref") : null;

        if (ref == null || !ref.containsKey("type")) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing completion ref");
        }

        String refType = ref.getString("type");
        String refName = ref.containsKey("name") ? ref.getString("name") : null;
        JsonObject argument = params.containsKey("argument") ? params.getJsonObject("argument") : null;
        String argName = argument != null && argument.containsKey("name") ? argument.getString("name") : null;
        String argValue = argument != null && argument.containsKey("value") ? argument.getString("value") : "";

        CompleteResult result;
        if ("ref/prompt".equals(refType) && refName != null && argName != null) {
            result = completePromptArgument(refName, argName, argValue);
        } else if ("ref/resource".equals(refType) && refName != null) {
            result = completeResourceUri(refName, argValue);
        } else {
            result = new CompleteResult(new CompleteResult.Completion(List.of(), 0, false));
        }

        return respond(request.getId(), result, sse);
    }

    @SuppressWarnings("unused") // TODO check
    private CompleteResult completePromptArgument(String promptName, String argName, String prefix) {
        return promptRegistry
                .findPrompt(promptName)
                .map(prompt -> {
                    List<String> matchingArgs = prompt.getArguments().stream()
                            .map(McpPromptDescriptor.PromptArgument::name)
                            .filter(name -> name.startsWith(prefix))
                            .toList();
                    return new CompleteResult(new CompleteResult.Completion(matchingArgs, matchingArgs.size(), false));
                })
                .orElse(new CompleteResult(new CompleteResult.Completion(List.of(), 0, false)));
    }

    @SuppressWarnings("unused") // TODO check
    private CompleteResult completeResourceUri(String uriTemplatePrefix, String prefix) {
        List<String> matchingUris = resourceRegistry.listResources().stream()
                .map(McpResourceDescriptor::getUri)
                .filter(uri -> uri.startsWith(prefix))
                .toList();
        return new CompleteResult(new CompleteResult.Completion(matchingUris, matchingUris.size(), false));
    }

    // --- Notifications ---

    private Response handleNotificationsCancelled(JsonRpcRequest request) {
        JsonObject params = request.getParams();
        if (params != null && params.containsKey("requestId")) {
            Object cancelledRequestId = extractJsonPrimitive(params.get("requestId"));
            if (cancelledRequestId != null) {
                cancellationManager.cancel(cancelledRequestId);
            }
        }
        return Response.ok().build();
    }

    private Object extractJsonPrimitive(JsonValue value) {
        if (value instanceof JsonString s) {
            return s.getString();
        }
        if (value.getValueType() == JsonValue.ValueType.NUMBER) {
            return ((jakarta.json.JsonNumber) value).longValue();
        }
        return value.toString();
    }

    @SuppressWarnings("unused") // TODO check
    private Response handleRootsListChanged(JsonRpcRequest request, String sessionId) {
        if (sessionId != null) {
            rootsManager.onRootsChanged(sessionId);
        }
        return Response.ok().build();
    }

    // --- Logging ---

    private Response handleLoggingSetLevel(JsonRpcRequest request, String sessionId) {
        sessionManager.requireSession(request.getId(), sessionId);

        JsonObject params = request.getParams();
        String level = params != null && params.containsKey("level") ? params.getString("level") : null;

        if (level == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Missing log level");
        }

        try {
            mcpLogger.setMinimumLevel(McpLogLevel.valueOf(level));
        } catch (IllegalArgumentException e) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_PARAMS, "Invalid log level: " + level);
        }

        return Response.ok(serializeToJson(JsonRpcResponse.success(request.getId(), Map.of())))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // --- Ping ---

    private Response handlePing(JsonRpcRequest request) {
        return Response.ok(serializeToJson(JsonRpcResponse.success(request.getId(), Map.of())))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // --- Shared ---

    private Response respond(Object id, Object result, boolean sse) {
        JsonRpcResponse rpcResponse = JsonRpcResponse.success(id, result);
        String json = serializeToJson(rpcResponse);
        if (!sse) {
            return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        }
        String payload = "event: message\ndata: " + json + "\n\n";
        StreamingOutput stream = out -> {
            out.write(payload.getBytes(StandardCharsets.UTF_8));
            out.flush();
        };
        return Response.ok(stream, MediaType.SERVER_SENT_EVENTS)
                .header(HEADER_CACHE_CONTROL, HEADER_NO_CACHE)
                .build();
    }

    private boolean isJsonRpcResponse(String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject json = reader.readObject();
            return !json.containsKey(FIELD_METHOD) && (json.containsKey(FIELD_RESULT) || json.containsKey(FIELD_ERROR));
        } catch (Exception e) {
            return false;
        }
    }

    private Response handleClientResponse(String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject json = reader.readObject();
            Object id = extractId(json);
            if (json.containsKey(FIELD_RESULT)) {
                JsonObject result = json.getJsonObject(FIELD_RESULT);
                serverRequestManager.handleResponse(id, result);
            } else if (json.containsKey(FIELD_ERROR)) {
                JsonObject error = json.getJsonObject(FIELD_ERROR);
                String message = error.containsKey("message") ? error.getString("message") : "Unknown error";
                serverRequestManager.handleErrorResponse(id, message);
            }
        }
        return Response.ok().build();
    }

    private String extractCursor(JsonObject params) {
        if (params != null && params.containsKey("cursor")) {
            return params.getString("cursor");
        }
        return null;
    }

    private McpServerConfig resolveConfig() {
        if (configInstance != null && configInstance.isResolvable()) {
            return configInstance.get();
        }
        return new McpServerConfig();
    }

    private JsonRpcRequest parseRequest(String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject json = reader.readObject();
            Object id = extractId(json);
            String method = json.containsKey(FIELD_METHOD) ? json.getString(FIELD_METHOD) : null;
            JsonObject params = json.containsKey("params") ? json.getJsonObject("params") : null;
            JsonRpcRequest request = new JsonRpcRequest(id, method, params);
            request.setProgressToken(extractProgressToken(params));
            return request;
        }
    }

    private Object extractProgressToken(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) {
            return null;
        }
        JsonObject meta = params.getJsonObject("_meta");
        if (meta == null || !meta.containsKey(FIELD_PROGRESS_TOKEN)) {
            return null;
        }
        JsonValue tokenValue = meta.get(FIELD_PROGRESS_TOKEN);
        if (tokenValue instanceof JsonString value) {
            return value.getString();
        }
        if (tokenValue.getValueType() == JsonValue.ValueType.NUMBER) {
            return meta.getJsonNumber(FIELD_PROGRESS_TOKEN).longValue();
        }
        return tokenValue.toString();
    }

    private Object extractId(JsonObject json) {
        if (!json.containsKey("id")) {
            return null;
        }
        JsonValue idValue = json.get("id");
        if (idValue instanceof JsonString value) {
            return value.getString();
        }
        if (idValue.getValueType() == JsonValue.ValueType.NUMBER) {
            return json.getJsonNumber("id").longValue();
        }
        return idValue.toString();
    }

    private String serializeToJson(Object obj) {
        JsonbConfig config = new JsonbConfig().withNullValues(false);
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            return jsonb.toJson(obj);
        } catch (Exception e) {
            throw new McpException(null, McpErrorCode.INTERNAL_ERROR, "JSON serialization failed");
        }
    }
}
