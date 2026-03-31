package dev.langchain4j.cdi.mcp.server.transport;

import dev.langchain4j.cdi.mcp.server.protocol.JsonRpcServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages server-initiated JSON-RPC requests to clients. Sends requests via SSE and correlates responses received back
 * via POST.
 */
@ApplicationScoped
public class McpServerRequestManager {

    private static final Logger LOGGER = Logger.getLogger(McpServerRequestManager.class.getName());
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final AtomicLong requestIdCounter = new AtomicLong(1);
    private final Map<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

    @Inject
    McpNotificationBroadcaster broadcaster;

    /**
     * Sends a JSON-RPC request to a specific client session and waits for the response.
     *
     * @param sessionId the target session
     * @param method the JSON-RPC method name (e.g. "roots/list", "sampling/createMessage")
     * @param params the request parameters (can be null)
     * @return the result JsonObject from the client's response, or null on timeout/error
     */
    public JsonObject sendRequest(String sessionId, String method, Object params) {
        return sendRequest(sessionId, method, params, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Sends a JSON-RPC request to a specific client session and waits for the response.
     *
     * @param sessionId the target session
     * @param method the JSON-RPC method name
     * @param params the request parameters
     * @param timeoutSeconds how long to wait for the response
     * @return the result JsonObject from the client's response, or null on timeout/error
     */
    public JsonObject sendRequest(String sessionId, String method, Object params, long timeoutSeconds) {
        String requestId = "server-" + requestIdCounter.getAndIncrement();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            JsonRpcServerRequest request = new JsonRpcServerRequest(requestId, method, params);
            broadcaster.sendToSession(sessionId, request);

            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "MCP: Server request " + method + " to session " + sessionId + " failed", e);
            return null;
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /**
     * Called when a JSON-RPC response (no method, has result/error) is received from a client. Completes the pending
     * future if one exists for the given id.
     *
     * @param id the response id
     * @param result the result object (null if error)
     * @return true if the response was matched to a pending request
     */
    public boolean handleResponse(Object id, JsonObject result) {
        if (id == null) {
            return false;
        }
        String key = id.toString();
        CompletableFuture<JsonObject> future = pendingRequests.get(key);
        if (future != null) {
            future.complete(result);
            return true;
        }
        return false;
    }

    /** Called when a JSON-RPC error response is received from a client. */
    public boolean handleErrorResponse(Object id, String errorMessage) {
        if (id == null) {
            return false;
        }
        String key = id.toString();
        CompletableFuture<JsonObject> future = pendingRequests.get(key);
        if (future != null) {
            future.completeExceptionally(new RuntimeException("Client error: " + errorMessage));
            return true;
        }
        return false;
    }

    public int pendingRequestCount() {
        return pendingRequests.size();
    }
}
