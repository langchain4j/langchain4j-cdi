package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpToolNotFoundException;
import dev.langchain4j.cdi.mcp.server.logging.McpLogLevel;
import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcRequest;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcResponse;
import dev.langchain4j.cdi.mcp.server.protocol.McpCursor;
import dev.langchain4j.cdi.mcp.server.protocol.McpImplementation;
import dev.langchain4j.cdi.mcp.server.protocol.McpInitializeResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpJsonSerializer;
import dev.langchain4j.cdi.mcp.server.protocol.McpListPromptsResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpListResourceTemplatesResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpListResourcesResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpListToolsResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpPagination;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptArgument;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptMessage;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptModel;
import dev.langchain4j.cdi.mcp.server.protocol.McpProtocol;
import dev.langchain4j.cdi.mcp.server.protocol.McpResourceModel;
import dev.langchain4j.cdi.mcp.server.protocol.McpResourceTemplateModel;
import dev.langchain4j.cdi.mcp.server.protocol.McpServerCapabilities;
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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
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
import org.mcpjava.server.prompts.PromptMessage;
import org.mcpjava.server.prompts.PromptResponse;
import org.mcpjava.server.resources.ResourceResponse;
import org.mcpjava.server.tools.ToolResponse;

/**
 * JAX-RS resource that implements the MCP Streamable HTTP transport at the {@code /mcp} endpoint. Handles JSON-RPC
 * requests over POST, SSE streaming over GET, and session termination over DELETE.
 */
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
    private static final String MCP_PROTOCOL_VERSION = McpProtocol.VERSION;

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

    /**
     * CDI injection constructor.
     *
     * @param toolRegistry registry of available MCP tools
     * @param resourceRegistry registry of available MCP resources
     * @param promptRegistry registry of available MCP prompts
     * @param sessionManager manages MCP sessions
     * @param toolInvoker invokes tool methods on CDI beans
     * @param beanInvoker invokes prompt and resource methods on CDI beans
     * @param broadcaster broadcasts SSE notifications to connected clients
     * @param mcpLogger MCP logging facade
     * @param subscriptionManager manages resource subscriptions
     * @param serverRequestManager manages server-initiated requests
     * @param rootsManager handles roots list changes
     * @param cancellationManager tracks cancellation state per request
     * @param configInstance optional server configuration bean
     */
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

    /**
     * Handles incoming JSON-RPC requests and client responses over HTTP POST.
     *
     * @param body the JSON-RPC request or response body
     * @param sessionId the MCP session identifier from the request header
     * @param accept the Accept header value used to determine SSE vs JSON response format
     * @return the JSON-RPC response, either as JSON or as an SSE event
     */
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

    /**
     * Opens an SSE stream for server-initiated notifications on an existing session.
     *
     * @param sessionId the MCP session identifier from the request header
     * @return an SSE streaming response, or 400 if no session ID is provided
     */
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

    /**
     * Terminates an MCP session.
     *
     * @param sessionId the MCP session identifier from the request header
     * @return an OK response after the session is terminated
     */
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

        McpInitializeResult result = new McpInitializeResult(
                MCP_PROTOCOL_VERSION,
                new McpServerCapabilities(
                        new McpServerCapabilities.ToolsCapability(true),
                        new McpServerCapabilities.ResourcesCapability(true, true),
                        new McpServerCapabilities.PromptsCapability(true),
                        McpServerCapabilities.LoggingCapability.INSTANCE),
                new McpImplementation(config.getServerName(), config.getServerVersion()));

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

        McpCursor nextCursor = page.nextCursor() != null ? new McpCursor(page.nextCursor()) : null;
        McpListToolsResult result = new McpListToolsResult(
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
            JsonObject result;
            if (callResult instanceof ToolResponse tr) {
                result = McpJsonSerializer.toolResponseToJson(tr);
            } else {
                result = McpJsonSerializer.plainTextToolResult(callResult);
            }
            return respondJson(request.getId(), result, sse);
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

        McpCursor nextCursor = page.nextCursor() != null ? new McpCursor(page.nextCursor()) : null;
        McpListResourcesResult result = new McpListResourcesResult(
                page.items().stream()
                        .map(r -> McpResourceModel.of(r.getUri(), r.getName(), r.getDescription(), r.getMimeType()))
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
            JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
            JsonArrayBuilder contentsArray = Json.createArrayBuilder();
            if (content instanceof ResourceResponse rr) {
                rr.getContents().forEach(rc -> contentsArray.add(McpJsonSerializer.resourceContentsToJson(rc)));
            } else {
                String text = content != null ? content.toString() : "";
                contentsArray.add(McpJsonSerializer.plainTextResourceContents(uri, text, resource.getMimeType()));
            }
            resultBuilder.add("contents", contentsArray);
            return respondJson(request.getId(), resultBuilder.build(), sse);
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

        McpCursor nextCursor = page.nextCursor() != null ? new McpCursor(page.nextCursor()) : null;
        McpListPromptsResult result = new McpListPromptsResult(
                page.items().stream()
                        .map(p -> McpPromptModel.of(
                                p.getName(),
                                p.getDescription(),
                                p.getArguments().stream()
                                        .map(a -> new McpPromptArgument(a.name(), a.description(), a.required()))
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
            JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
            resultBuilder.add("description", prompt.getDescription());
            JsonArrayBuilder msgsArray = Json.createArrayBuilder();
            if (callResult instanceof PromptResponse pr) {
                pr.messages().forEach(m -> msgsArray.add(McpJsonSerializer.promptMessageToJson(m)));
            } else if (callResult instanceof List<?> messages) {
                for (Object msg : messages) {
                    if (msg instanceof PromptMessage pm) {
                        msgsArray.add(McpJsonSerializer.promptMessageToJson(pm));
                    } else if (msg instanceof McpPromptMessage mpm) {
                        msgsArray.add(McpJsonSerializer.contentBlockMessageToJson(mpm.role(), mpm.content()));
                    } else if (msg != null) {
                        msgsArray.add(McpJsonSerializer.plainTextPromptMessage(msg.toString()));
                    }
                }
            } else {
                String text = callResult != null ? callResult.toString() : "";
                msgsArray.add(McpJsonSerializer.plainTextPromptMessage(text));
            }
            resultBuilder.add("messages", msgsArray);
            return respondJson(request.getId(), resultBuilder.build(), sse);
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

        McpCursor nextCursor = page.nextCursor() != null ? new McpCursor(page.nextCursor()) : null;
        McpListResourceTemplatesResult result = new McpListResourceTemplatesResult(
                page.items().stream()
                        .map(t -> McpResourceTemplateModel.of(
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

        List<String> completionValues;
        if ("ref/prompt".equals(refType) && refName != null && argName != null) {
            completionValues = completePromptArgument(refName, argName, argValue);
        } else if ("ref/resource".equals(refType) && refName != null) {
            completionValues = completeResourceUri(refName, argValue);
        } else {
            completionValues = List.of();
        }

        return respondJson(request.getId(), buildCompletionResultJson(completionValues), sse);
    }

    private List<String> completePromptArgument(String promptName, String argName, String prefix) {
        return promptRegistry
                .findPrompt(promptName)
                .map(prompt -> prompt.getArguments().stream()
                        .map(McpPromptDescriptor.PromptArgument::name)
                        .filter(name -> name.startsWith(prefix))
                        .toList())
                .orElse(List.of());
    }

    private List<String> completeResourceUri(String uriTemplatePrefix, String prefix) {
        return resourceRegistry.listResources().stream()
                .map(McpResourceDescriptor::getUri)
                .filter(uri -> uri.startsWith(prefix))
                .toList();
    }

    private JsonObject buildCompletionResultJson(List<String> values) {
        JsonArrayBuilder valuesArray = Json.createArrayBuilder();
        values.forEach(valuesArray::add);
        return Json.createObjectBuilder()
                .add(
                        "completion",
                        Json.createObjectBuilder()
                                .add("values", valuesArray)
                                .add("total", values.size())
                                .add("hasMore", false))
                .build();
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

    private Response respondJson(Object id, JsonObject result, boolean sse) {
        JsonObjectBuilder rpc = Json.createObjectBuilder().add("jsonrpc", "2.0");
        addJsonRpcId(rpc, id);
        rpc.add("result", result);
        return sendResponse(rpc.build().toString(), sse);
    }

    private Response respond(Object id, Object result, boolean sse) {
        return sendResponse(serializeToJson(JsonRpcResponse.success(id, result)), sse);
    }

    private static void addJsonRpcId(JsonObjectBuilder builder, Object id) {
        if (id instanceof String s) {
            builder.add("id", s);
        } else if (id instanceof Number n) {
            builder.add("id", n.longValue());
        }
    }

    private Response sendResponse(String json, boolean sse) {
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
