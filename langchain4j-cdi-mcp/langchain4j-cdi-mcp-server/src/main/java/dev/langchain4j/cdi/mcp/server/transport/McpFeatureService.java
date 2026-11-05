package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpInvalidArgumentException;
import dev.langchain4j.cdi.mcp.server.error.McpToolNotFoundException;
import dev.langchain4j.cdi.mcp.server.protocol.McpCursor;
import dev.langchain4j.cdi.mcp.server.protocol.McpImplementation;
import dev.langchain4j.cdi.mcp.server.protocol.McpJsonSerializer;
import dev.langchain4j.cdi.mcp.server.protocol.McpListPromptsResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpListResourceTemplatesResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpListResourcesResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpListToolsResult;
import dev.langchain4j.cdi.mcp.server.protocol.McpPagination;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptArgument;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptMessage;
import dev.langchain4j.cdi.mcp.server.protocol.McpPromptModel;
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
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mcpjava.server.prompts.PromptMessage;
import org.mcpjava.server.prompts.PromptResponse;
import org.mcpjava.server.resources.ResourceResponse;
import org.mcpjava.server.tools.ToolResponse;

/**
 * Business logic shared by every protocol era: tool/resource/prompt listing and invocation, and completion. This
 * service is transport-agnostic - it never touches {@code jakarta.ws.rs.core.Response} and never validates session
 * state (that remains the caller's responsibility).
 */
@ApplicationScoped
public class McpFeatureService {

    private McpToolRegistry toolRegistry;
    private McpResourceRegistry resourceRegistry;
    private McpPromptRegistry promptRegistry;
    private McpToolInvoker toolInvoker;
    private McpBeanInvoker beanInvoker;
    private McpCancellationManager cancellationManager;
    private McpServerConfigResolver config;

    /** No-arg constructor required by CDI proxying. */
    public McpFeatureService() {}

    /**
     * CDI injection constructor.
     *
     * @param toolRegistry registry of available MCP tools
     * @param resourceRegistry registry of available MCP resources
     * @param promptRegistry registry of available MCP prompts
     * @param toolInvoker invokes tool methods on CDI beans
     * @param beanInvoker invokes prompt and resource methods on CDI beans
     * @param cancellationManager tracks cancellation state per request
     * @param config resolves the server configuration
     */
    @Inject
    public McpFeatureService(
            McpToolRegistry toolRegistry,
            McpResourceRegistry resourceRegistry,
            McpPromptRegistry promptRegistry,
            McpToolInvoker toolInvoker,
            McpBeanInvoker beanInvoker,
            McpCancellationManager cancellationManager,
            McpServerConfigResolver config) {
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.promptRegistry = promptRegistry;
        this.toolInvoker = toolInvoker;
        this.beanInvoker = beanInvoker;
        this.cancellationManager = cancellationManager;
        this.config = config;
    }

    /**
     * Returns the capabilities advertised by this server during initialization.
     *
     * @return the server capabilities
     */
    public McpServerCapabilities capabilities() {
        return new McpServerCapabilities(
                new McpServerCapabilities.ToolsCapability(true),
                new McpServerCapabilities.ResourcesCapability(true, true),
                new McpServerCapabilities.PromptsCapability(true),
                McpServerCapabilities.LoggingCapability.INSTANCE,
                McpServerCapabilities.CompletionsCapability.INSTANCE);
    }

    /**
     * Returns the server implementation info advertised during initialization.
     *
     * @return the server implementation info
     */
    public McpImplementation serverInfo() {
        McpServerConfig c = config.get();
        return new McpImplementation(c.getServerName(), c.getServerVersion());
    }

    /**
     * Looks up a registered tool by name.
     *
     * <p>Used by the 2026-07-28 path to read a tool's SEP-2243 {@code x-mcp-header} designations before dispatching a
     * {@code tools/call}, without re-deriving them from the bean's annotations on every request.
     *
     * @param name the tool name
     * @return the descriptor, or empty when no tool is registered under that name
     */
    public Optional<McpToolDescriptor> findTool(String name) {
        return toolRegistry.findTool(name);
    }

    /**
     * Extracts the pagination cursor from a request's parameters.
     *
     * @param params the request parameters, may be {@code null}
     * @return the cursor, or {@code null} if absent
     */
    public static String cursor(JsonObject params) {
        return params != null ? params.getString("cursor", null) : null;
    }

    /**
     * Lists the registered tools.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @return the {@code tools/list} result as a JSON object
     */
    public JsonObject listTools(String cursor) {
        return listTools(cursor, false);
    }

    /**
     * Lists the registered tools for a given protocol era.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @param modernEra {@code true} for MCP 2026-07-28, whose tool schemas carry the SEP-2243 {@code x-mcp-header}
     *     argument designations; {@code false} for the 2025-03-26 legacy era, which predates SEP-2243 and whose output
     *     is unchanged by it
     * @return the {@code tools/list} result as a JSON object
     */
    public JsonObject listTools(String cursor, boolean modernEra) {
        McpPagination.Page<McpToolDescriptor> page =
                McpPagination.paginate(new ArrayList<>(toolRegistry.listTools()), cursor);
        return McpJsonSerializer.toJsonObject(new McpListToolsResult(
                page.items().stream()
                        .map(descriptor -> descriptor.toWireFormat(modernEra))
                        .toList(),
                nextCursor(page)));
    }

    /**
     * Lists the registered resources.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @return the {@code resources/list} result as a JSON object
     */
    public JsonObject listResources(String cursor) {
        return listResources(cursor, false);
    }

    /**
     * Lists the registered resources for a given protocol era.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @param modernEra {@code true} for MCP 2026-07-28, whose {@code Resource} definition carries {@code icons};
     *     {@code false} for the 2025-03-26 legacy era, which has no such member and whose output is therefore unchanged
     * @return the {@code resources/list} result as a JSON object
     */
    public JsonObject listResources(String cursor, boolean modernEra) {
        McpPagination.Page<McpResourceDescriptor> page =
                McpPagination.paginate(new ArrayList<>(resourceRegistry.listResources()), cursor);
        McpListResourcesResult result = new McpListResourcesResult(
                page.items().stream()
                        .map(r -> McpResourceModel.of(
                                r.getUri(),
                                r.getName(),
                                r.getDescription(),
                                r.getMimeType(),
                                modernEra ? r.getIcons() : null))
                        .toList(),
                nextCursor(page));
        return McpJsonSerializer.toJsonObject(result);
    }

    /**
     * Lists the registered resource templates.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @return the {@code resources/templates/list} result as a JSON object
     */
    public JsonObject listResourceTemplates(String cursor) {
        return listResourceTemplates(cursor, false);
    }

    /**
     * Lists the registered resource templates for a given protocol era.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @param modernEra {@code true} for MCP 2026-07-28, whose {@code ResourceTemplate} definition carries
     *     {@code icons}; {@code false} for the 2025-03-26 legacy era, which has no such member and whose output is
     *     therefore unchanged
     * @return the {@code resources/templates/list} result as a JSON object
     */
    public JsonObject listResourceTemplates(String cursor, boolean modernEra) {
        McpPagination.Page<McpResourceTemplateDescriptor> page =
                McpPagination.paginate(new ArrayList<>(resourceRegistry.listTemplates()), cursor);
        McpListResourceTemplatesResult result = new McpListResourceTemplatesResult(
                page.items().stream()
                        .map(t -> McpResourceTemplateModel.of(
                                t.getUriTemplate(),
                                t.getName(),
                                t.getDescription(),
                                t.getMimeType(),
                                modernEra ? t.getIcons() : null))
                        .toList(),
                nextCursor(page));
        return McpJsonSerializer.toJsonObject(result);
    }

    /**
     * Lists the registered prompts.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @return the {@code prompts/list} result as a JSON object
     */
    public JsonObject listPrompts(String cursor) {
        return listPrompts(cursor, false);
    }

    /**
     * Lists the registered prompts for a given protocol era.
     *
     * @param cursor the pagination cursor, or {@code null} for the first page
     * @param modernEra {@code true} for MCP 2026-07-28, whose {@code Prompt} definition carries {@code icons};
     *     {@code false} for the 2025-03-26 legacy era, which has no such member and whose output is therefore unchanged
     * @return the {@code prompts/list} result as a JSON object
     */
    public JsonObject listPrompts(String cursor, boolean modernEra) {
        McpPagination.Page<McpPromptDescriptor> page =
                McpPagination.paginate(new ArrayList<>(promptRegistry.listPrompts()), cursor);
        McpListPromptsResult result = new McpListPromptsResult(
                page.items().stream()
                        .map(p -> McpPromptModel.of(
                                p.getName(),
                                p.getDescription(),
                                p.getArguments().stream()
                                        .map(a -> new McpPromptArgument(a.name(), a.description(), a.required()))
                                        .toList(),
                                modernEra ? p.getIcons() : null))
                        .toList(),
                nextCursor(page));
        return McpJsonSerializer.toJsonObject(result);
    }

    /**
     * Invokes a tool by name.
     *
     * <p>An argument that cannot be bound to its parameter ({@link McpInvalidArgumentException}) is answered by era. In
     * MCP 2026-07-28 it is a tool execution error, a result with {@code isError: true} whose text names the argument,
     * so the model can correct the call ({@code server/tools.mdx}, "Error Handling", since 2025-11-25). In 2025-03-26
     * it is thrown as {@code -32602 Invalid params} ("Invalid arguments" is a protocol error there).
     *
     * @param requestId the JSON-RPC request id
     * @param params the {@code tools/call} parameters ({@code name}, {@code arguments})
     * @param ctx the request context (progress, cancellation, client requester, response channel)
     * @param session the MCP session, or {@code null}
     * @return the {@code tools/call} result as a JSON object
     */
    public JsonObject callTool(Object requestId, JsonObject params, McpRequestContext ctx, McpSession session) {
        String toolName = params != null ? params.getString("name", null) : null;
        if (toolName == null) {
            throw new McpException(requestId, McpErrorCode.INVALID_PARAMS, "Missing tool name");
        }
        JsonObject arguments = params.get("arguments") instanceof JsonObject a ? a : null;
        McpToolDescriptor tool =
                toolRegistry.findTool(toolName).orElseThrow(() -> new McpToolNotFoundException(requestId, toolName));
        cancellationManager.register(requestId, ctx.cancelledFlag());
        try {
            Object callResult = toolInvoker.invoke(requestId, tool, arguments, ctx, session);
            return callResult instanceof ToolResponse tr
                    ? McpJsonSerializer.toolResponseToJson(tr)
                    : McpJsonSerializer.plainTextToolResult(callResult);
        } catch (McpInvalidArgumentException e) {
            if (ctx.isModern()) {
                return invalidArgumentResult(e);
            }
            throw withRequestId(requestId, e);
        } catch (McpException e) {
            throw withRequestId(requestId, e);
        } finally {
            cancellationManager.unregister(requestId);
        }
    }

    private static JsonObject invalidArgumentResult(McpInvalidArgumentException e) {
        return Json.createObjectBuilder(McpJsonSerializer.plainTextToolResult(e.getMessage()))
                .add("isError", true)
                .build();
    }

    /**
     * Reads a resource by URI. An exact registered URI is served first; failing that, the URI is matched against the
     * registered {@code @ResourceTemplate} URI templates and the variables it carries are bound to the template
     * method's parameters by name.
     *
     * @param requestId the JSON-RPC request id
     * @param params the {@code resources/read} parameters ({@code uri})
     * @param ctx the request context
     * @param session the MCP session, or {@code null}
     * @return the {@code resources/read} result as a JSON object
     */
    public JsonObject readResource(Object requestId, JsonObject params, McpRequestContext ctx, McpSession session) {
        String uri = params != null && params.containsKey("uri") ? params.getString("uri") : null;

        if (uri == null) {
            throw new McpException(requestId, McpErrorCode.INVALID_PARAMS, "Missing resource URI");
        }

        Optional<McpResourceDescriptor> resource = resourceRegistry.findResource(uri);
        if (resource.isPresent()) {
            return read(
                    requestId,
                    uri,
                    resource.get().getBeanType(),
                    resource.get().getMethod(),
                    null,
                    resource.get().getMimeType(),
                    ctx,
                    session);
        }

        Optional<McpResourceRegistry.TemplateMatch> match = resourceRegistry.matchTemplate(uri);
        if (match.isPresent()) {
            McpResourceTemplateDescriptor template = match.get().template();
            return read(
                    requestId,
                    uri,
                    template.getBeanType(),
                    template.getMethod(),
                    templateArguments(match.get().variables()),
                    template.getMimeType(),
                    ctx,
                    session);
        }

        throw resourceNotFound(requestId, uri);
    }

    /**
     * Builds the {@code -32602} error answered for an unknown resource URI. SEP-2164 asks the error to carry the URI
     * that was requested in its {@code data}, so a client can tell which of several reads failed.
     *
     * @param requestId the JSON-RPC request id
     * @param uri the requested resource URI
     * @return the exception to throw
     */
    private static McpException resourceNotFound(Object requestId, String uri) {
        return new McpException(
                requestId, McpErrorCode.INVALID_PARAMS, "Resource not found: " + uri, 200, Map.of("uri", uri));
    }

    private static JsonObject templateArguments(Map<String, String> variables) {
        JsonObjectBuilder arguments = Json.createObjectBuilder();
        variables.forEach(arguments::add);
        return arguments.build();
    }

    private JsonObject read(
            Object requestId,
            String uri,
            Class<?> beanType,
            Method method,
            JsonObject arguments,
            String mimeType,
            McpRequestContext ctx,
            McpSession session) {
        try {
            Object content = beanInvoker.invoke(requestId, beanType, method, arguments, ctx, session);
            JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
            JsonArrayBuilder contentsArray = Json.createArrayBuilder();
            if (content instanceof ResourceResponse rr) {
                rr.getContents().forEach(rc -> contentsArray.add(McpJsonSerializer.resourceContentsToJson(rc)));
            } else {
                String text = content != null ? content.toString() : "";
                contentsArray.add(McpJsonSerializer.plainTextResourceContents(uri, text, mimeType));
            }
            resultBuilder.add("contents", contentsArray);
            return resultBuilder.build();
        } catch (McpException e) {
            throw withRequestId(requestId, e);
        }
    }

    /**
     * Renders a prompt by name.
     *
     * @param requestId the JSON-RPC request id
     * @param params the {@code prompts/get} parameters ({@code name}, {@code arguments})
     * @param ctx the request context
     * @param session the MCP session, or {@code null}
     * @return the {@code prompts/get} result as a JSON object
     */
    public JsonObject getPrompt(Object requestId, JsonObject params, McpRequestContext ctx, McpSession session) {
        String promptName = params != null && params.containsKey("name") ? params.getString("name") : null;

        if (promptName == null) {
            throw new McpException(requestId, McpErrorCode.INVALID_PARAMS, "Missing prompt name");
        }

        JsonObject arguments = params.containsKey("arguments") ? params.getJsonObject("arguments") : null;

        McpPromptDescriptor prompt = promptRegistry
                .findPrompt(promptName)
                .orElseThrow(() ->
                        new McpException(requestId, McpErrorCode.INVALID_PARAMS, "Prompt not found: " + promptName));

        try {
            Object callResult =
                    beanInvoker.invoke(requestId, prompt.getBeanType(), prompt.getMethod(), arguments, ctx, session);
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
            return resultBuilder.build();
        } catch (McpException e) {
            throw withRequestId(requestId, e);
        }
    }

    /**
     * Computes completion suggestions for a prompt argument or resource URI.
     *
     * @param requestId the JSON-RPC request id
     * @param params the {@code completion/complete} parameters ({@code ref}, {@code argument})
     * @return the {@code completion/complete} result as a JSON object
     */
    public JsonObject complete(Object requestId, JsonObject params) {
        JsonObject ref = params != null && params.containsKey("ref") ? params.getJsonObject("ref") : null;

        if (ref == null || !ref.containsKey("type")) {
            throw new McpException(requestId, McpErrorCode.INVALID_PARAMS, "Missing completion ref");
        }

        String refType = ref.getString("type");
        String refName = ref.containsKey("name") ? ref.getString("name") : null;
        JsonObject argument = params.containsKey("argument") ? params.getJsonObject("argument") : null;
        String argName = argument != null && argument.containsKey("name") ? argument.getString("name") : null;
        String argValue = argument != null && argument.containsKey("value") ? argument.getString("value") : "";

        List<String> completionValues;
        if ("ref/prompt".equals(refType) && refName != null && argName != null) {
            completionValues = completePromptArgument(refName, argValue);
        } else if ("ref/resource".equals(refType) && refName != null) {
            completionValues = completeResourceUri(argValue);
        } else {
            completionValues = List.of();
        }

        return buildCompletionResultJson(completionValues);
    }

    private List<String> completePromptArgument(String promptName, String prefix) {
        return promptRegistry
                .findPrompt(promptName)
                .map(prompt -> prompt.getArguments().stream()
                        .map(McpPromptDescriptor.PromptArgument::name)
                        .filter(name -> name.startsWith(prefix))
                        .toList())
                .orElse(List.of());
    }

    private List<String> completeResourceUri(String prefix) {
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

    /**
     * Fills in the request id of an error raised deeper in the call chain — where the id is not known — so that the
     * client can match the answer to its request. Everything else is carried over unchanged, the cause included: this
     * is the same failure, not a new one.
     *
     * @param requestId the JSON-RPC request id of the request being served
     * @param e the error raised by the invocation
     * @return the error to answer, with its request id set
     */
    private static McpException withRequestId(Object requestId, McpException e) {
        if (e.getRequestId() != null) {
            return e;
        }
        return new McpException(
                requestId, e.getErrorCode(), e.getMessage(), e.getHttpStatus(), e.getData(), e.getCause());
    }

    private static McpCursor nextCursor(McpPagination.Page<?> page) {
        return page.nextCursor() != null ? new McpCursor(page.nextCursor()) : null;
    }
}
