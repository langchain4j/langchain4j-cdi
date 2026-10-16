package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.error.McpProtocolErrors;
import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcRequest;
import dev.langchain4j.cdi.mcp.server.protocol.McpHttpHeaders;
import dev.langchain4j.cdi.mcp.server.protocol.McpProtocolVersions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAX-RS resource that implements the MCP Streamable HTTP transport at the {@code /mcp} endpoint. Validates the
 * {@code Origin} header, detects the protocol era of each request, and routes legacy (2025-03-26) requests to
 * {@link McpLegacyProtocolHandler} and modern (2026-07-28) requests to {@link McpModernProtocolHandler}.
 *
 * <p>Long-lived streams (the legacy {@code GET} notification stream and modern {@code subscriptions/listen} streams)
 * are written through a Jakarta REST {@link SseEventSink}, which the runtime flushes after every event; raw response
 * output streams may be held in a runtime output buffer until it fills up. {@link McpListenRoutingFilter} routes
 * {@code subscriptions/listen} requests to {@link #handleListen}.
 */
@Path("/mcp")
@ApplicationScoped
public class McpEndpoint {

    private static final Logger LOGGER = Logger.getLogger(McpEndpoint.class.getName());

    /** Internal sub-path serving {@code subscriptions/listen} requests routed by {@link McpListenRoutingFilter}. */
    public static final String LISTEN_PATH = "_listen";

    private static final String FORBIDDEN_BODY =
            "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600,\"message\":\"Forbidden: invalid Origin header\"}}";

    @Inject
    McpLegacyProtocolHandler legacy;

    @Inject
    McpModernProtocolHandler modern;

    @Inject
    McpServerConfigResolver config;

    /** No-arg constructor required by CDI proxying and JAX-RS runtimes. */
    public McpEndpoint() {}

    /**
     * Handles incoming JSON-RPC requests and client responses over HTTP POST.
     *
     * @param body the JSON-RPC request or response body
     * @param headers the HTTP request headers
     * @return the JSON-RPC response, either as JSON or as an SSE event
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.SERVER_SENT_EVENTS})
    public Response handlePost(String body, @Context HttpHeaders headers) {
        Response forbidden = rejectInvalidOrigin(headers);
        if (forbidden != null) {
            return forbidden;
        }
        JsonObject json = McpJsonRpcParser.parseJson(body);
        if (McpJsonRpcParser.isJsonRpcResponse(json)) {
            return legacy.handleClientResponse(body);
        }
        JsonRpcRequest request = McpJsonRpcParser.parseRequest(json);
        if (request.getMethod() == null) {
            throw new McpException(request.getId(), McpErrorCode.INVALID_REQUEST, "Missing method");
        }
        String sessionId = headers.getHeaderString(McpHttpHeaders.SESSION_ID);
        String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
        boolean wantsSse = accept != null && accept.contains(MediaType.SERVER_SENT_EVENTS);

        if (request.getId() == null
                && sessionId == null
                && McpProtocolVersions.isModernEra(headers.getHeaderString(McpHttpHeaders.PROTOCOL_VERSION))) {
            return toResponse(McpReply.accepted());
        }
        McpProtocolContext protocol = McpEraDetector.detect(request, headers::getHeaderString);
        if (protocol.isModern()) {
            // the header lookup is passed on so that the SEP-2243 Mcp-Param-* headers can be validated against the
            // body before a tools/call is dispatched; the legacy branch below predates SEP-2243 and is untouched
            return toResponse(modern.handle(request, protocol, wantsSse, headers::getHeaderString));
        }
        return legacy.handle(request, sessionId);
    }

    /**
     * Serves a modern {@code subscriptions/listen} request, routed here by {@link McpListenRoutingFilter}. The request
     * is fully validated (Origin, protocol headers, method) before any event is sent, so validation failures are
     * returned as plain HTTP error responses. The method returns once the subscription is acknowledged; the stream
     * stays open, without holding the request thread, until the server shuts down or the client is found gone.
     *
     * @param body the JSON-RPC request body
     * @param headers the HTTP request headers
     * @param sink the event sink of the response
     * @param sse the Jakarta REST SSE entry point
     */
    @POST
    @Path(LISTEN_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @McpSseStream
    public void handleListen(String body, @Context HttpHeaders headers, @Context SseEventSink sink, @Context Sse sse) {
        JsonRpcRequest request = validateListen(body, headers);
        McpSseEventSinkChannel channel = new McpSseEventSinkChannel(sink, sse, new AtomicBoolean());
        try {
            modern.listen(request, channel);
        } catch (McpException e) {
            channel.close();
            throw e;
        } catch (RuntimeException e) {
            channel.close();
            throw McpModernProtocolHandler.internalError(request.getId(), request.getMethod(), e);
        }
    }

    private JsonRpcRequest validateListen(String body, HttpHeaders headers) {
        try {
            Response forbidden = rejectInvalidOrigin(headers);
            if (forbidden != null) {
                throw new WebApplicationException(forbidden);
            }
            JsonObject json = McpJsonRpcParser.parseJson(body);
            JsonRpcRequest request =
                    McpJsonRpcParser.isJsonRpcResponse(json) ? null : McpJsonRpcParser.parseRequest(json);
            if (request == null || request.getMethod() == null || request.getId() == null) {
                throw new McpException(
                        request != null ? request.getId() : null,
                        McpErrorCode.INVALID_REQUEST,
                        McpListenRoutingFilter.LISTEN_METHOD + " must be a JSON-RPC request with an id");
            }
            McpProtocolContext protocol = McpEraDetector.detect(request, headers::getHeaderString);
            if (!protocol.isModern() || !McpListenRoutingFilter.LISTEN_METHOD.equals(request.getMethod())) {
                throw McpProtocolErrors.methodNotFound(request.getId(), request.getMethod());
            }
            return request;
        } catch (McpException | WebApplicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw McpModernProtocolHandler.internalError(null, McpListenRoutingFilter.LISTEN_METHOD, e);
        }
    }

    private static Response toResponse(McpReply reply) {
        if (reply.isStream()) {
            StreamingOutput output = out -> {
                try {
                    reply.stream().write(out);
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "MCP: SSE response stream closed by client", e);
                }
            };
            return Response.ok(output, MediaType.SERVER_SENT_EVENTS)
                    .header("Cache-Control", "no-cache")
                    .header("X-Accel-Buffering", "no")
                    .build();
        }
        Response.ResponseBuilder builder = Response.status(reply.status());
        if (reply.body() != null) {
            builder.entity(reply.body()).type(reply.contentType());
        }
        return builder.build();
    }

    /**
     * Opens an SSE stream for server-initiated notifications on an existing session. The session is validated before
     * any event is sent, so a missing session ID (400), an invalid Origin (403) or an unknown session are returned as
     * plain HTTP error responses. The method returns once the stream is opened; the stream stays open, without holding
     * the request thread, until the server shuts down or the client is found gone.
     *
     * @param headers the HTTP request headers
     * @param sink the event sink of the response
     * @param sse the Jakarta REST SSE entry point
     */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @McpSseStream
    public void handleGet(@Context HttpHeaders headers, @Context SseEventSink sink, @Context Sse sse) {
        Response forbidden = rejectInvalidOrigin(headers);
        if (forbidden != null) {
            throw new WebApplicationException(forbidden);
        }
        String sessionId = headers.getHeaderString(McpHttpHeaders.SESSION_ID);
        legacy.validateStream(sessionId);
        McpSseEventSinkChannel channel = new McpSseEventSinkChannel(sink, sse, null);
        try {
            legacy.openStream(sessionId, channel);
        } catch (RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    /**
     * Terminates an MCP session.
     *
     * @param headers the HTTP request headers
     * @return an OK response after the session is terminated
     */
    @DELETE
    public Response handleDelete(@Context HttpHeaders headers) {
        Response forbidden = rejectInvalidOrigin(headers);
        return forbidden != null ? forbidden : legacy.terminate(headers.getHeaderString(McpHttpHeaders.SESSION_ID));
    }

    private Response rejectInvalidOrigin(HttpHeaders headers) {
        boolean allowed = McpOriginValidator.isAllowed(
                headers.getHeaderString(McpHttpHeaders.ORIGIN),
                headers.getHeaderString(McpHttpHeaders.HOST),
                config.get().getAllowedOrigins());
        return allowed
                ? null
                : Response.status(Response.Status.FORBIDDEN)
                        .entity(FORBIDDEN_BODY)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
    }
}
