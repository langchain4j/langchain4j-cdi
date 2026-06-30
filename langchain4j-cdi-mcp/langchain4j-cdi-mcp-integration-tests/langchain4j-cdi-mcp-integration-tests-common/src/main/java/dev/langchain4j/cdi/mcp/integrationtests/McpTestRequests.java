package dev.langchain4j.cdi.mcp.integrationtests;

/** Factory for JSON-RPC request bodies used in MCP integration tests. */
public final class McpTestRequests {

    private McpTestRequests() {}

    // --- Lifecycle ---

    /**
     * Builds an {@code initialize} request.
     *
     * @param id the JSON-RPC request id
     * @return the request body
     */
    public static String initializeRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0\"}}}"
                .formatted(formatId(id));
    }

    /**
     * Builds an {@code initialized} notification.
     *
     * @return the notification body
     */
    @SuppressWarnings("java:S3400")
    public static String initializedNotification() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}";
    }

    /**
     * Builds a {@code ping} request.
     *
     * @param id the JSON-RPC request id
     * @return the request body
     */
    public static String pingRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"ping\",\"params\":{}}".formatted(formatId(id));
    }

    // --- Tools ---

    /**
     * Builds a {@code tools/list} request.
     *
     * @param id the JSON-RPC request id
     * @return the request body
     */
    public static String toolsListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"tools/list\",\"params\":{}}".formatted(formatId(id));
    }

    /**
     * Builds a {@code tools/call} request.
     *
     * @param id the JSON-RPC request id
     * @param toolName the tool to call
     * @param argsJson the tool arguments as JSON
     * @return the request body
     */
    public static String toolsCallRequest(Object id, String toolName, String argsJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"tools/call\",\"params\":{\"name\":\"%s\",\"arguments\":%s}}"
                .formatted(formatId(id), toolName, argsJson);
    }

    // --- Resources ---

    /**
     * Builds a {@code resources/list} request.
     *
     * @param id the JSON-RPC request id
     * @return the request body
     */
    public static String resourcesListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/list\",\"params\":{}}".formatted(formatId(id));
    }

    /**
     * Builds a {@code resources/read} request.
     *
     * @param id the JSON-RPC request id
     * @param uri the resource URI
     * @return the request body
     */
    public static String resourcesReadRequest(Object id, String uri) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/read\",\"params\":{\"uri\":\"%s\"}}"
                .formatted(formatId(id), uri);
    }

    /**
     * Builds a {@code resources/subscribe} request.
     *
     * @param id the JSON-RPC request id
     * @param uri the resource URI
     * @return the request body
     */
    public static String resourcesSubscribeRequest(Object id, String uri) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/subscribe\",\"params\":{\"uri\":\"%s\"}}"
                .formatted(formatId(id), uri);
    }

    /**
     * Builds a {@code resources/unsubscribe} request.
     *
     * @param id the JSON-RPC request id
     * @param uri the resource URI
     * @return the request body
     */
    public static String resourcesUnsubscribeRequest(Object id, String uri) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/unsubscribe\",\"params\":{\"uri\":\"%s\"}}"
                .formatted(formatId(id), uri);
    }

    /**
     * Builds a {@code resources/templates/list} request.
     *
     * @param id the JSON-RPC request id
     * @return the request body
     */
    public static String resourcesTemplatesListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/templates/list\",\"params\":{}}"
                .formatted(formatId(id));
    }

    // --- Prompts ---

    /**
     * Builds a {@code prompts/list} request.
     *
     * @param id the JSON-RPC request id
     * @return the request body
     */
    public static String promptsListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"prompts/list\",\"params\":{}}".formatted(formatId(id));
    }

    /**
     * Builds a {@code prompts/get} request with arguments.
     *
     * @param id the JSON-RPC request id
     * @param name the prompt name
     * @param argsJson the prompt arguments as JSON
     * @return the request body
     */
    public static String promptsGetRequest(Object id, String name, String argsJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"prompts/get\",\"params\":{\"name\":\"%s\",\"arguments\":%s}}"
                .formatted(formatId(id), name, argsJson);
    }

    /**
     * Builds a {@code prompts/get} request without arguments.
     *
     * @param id the JSON-RPC request id
     * @param name the prompt name
     * @return the request body
     */
    public static String promptsGetRequest(Object id, String name) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"prompts/get\",\"params\":{\"name\":\"%s\"}}"
                .formatted(formatId(id), name);
    }

    // --- Completion ---

    /**
     * Builds a {@code completion/complete} request.
     *
     * @param id the JSON-RPC request id
     * @param refType the reference type
     * @param refName the reference name
     * @param argName the argument name
     * @param argValue the argument value
     * @return the request body
     */
    public static String completionCompleteRequest(
            Object id, String refType, String refName, String argName, String argValue) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"completion/complete\",\"params\":{\"ref\":{\"type\":\"%s\",\"name\":\"%s\"},\"argument\":{\"name\":\"%s\",\"value\":\"%s\"}}}"
                .formatted(formatId(id), refType, refName, argName, argValue);
    }

    // --- Logging ---

    /**
     * Builds a {@code logging/setLevel} request.
     *
     * @param id the JSON-RPC request id
     * @param level the log level to set
     * @return the request body
     */
    public static String loggingSetLevelRequest(Object id, String level) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"logging/setLevel\",\"params\":{\"level\":\"%s\"}}"
                .formatted(formatId(id), level);
    }

    // --- Notifications ---

    /**
     * Builds a {@code notifications/cancelled} notification.
     *
     * @param requestId the id of the request to cancel
     * @param reason the cancellation reason
     * @return the notification body
     */
    public static String cancelledNotification(String requestId, String reason) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":\"%s\",\"reason\":\"%s\"}}"
                .formatted(requestId, reason);
    }

    /**
     * Builds a {@code notifications/roots/list_changed} notification.
     *
     * @return the notification body
     */
    @SuppressWarnings("java:S3400")
    public static String rootsListChangedNotification() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/roots/list_changed\",\"params\":{}}";
    }

    // --- Unknown / Error scenarios ---

    /**
     * Builds a request with an unknown method name.
     *
     * @param id the JSON-RPC request id
     * @param method the method name
     * @return the request body
     */
    public static String unknownMethodRequest(Object id, String method) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"%s\",\"params\":{}}".formatted(formatId(id), method);
    }

    /**
     * Builds a client-initiated JSON-RPC response (e.g. for roots).
     *
     * @param id the JSON-RPC id matching the server's request
     * @param resultJson the result payload as JSON
     * @return the response body
     */
    public static String clientJsonRpcResponse(Object id, String resultJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"result\":%s}".formatted(formatId(id), resultJson);
    }

    private static String formatId(Object id) {
        if (id instanceof String) {
            return "\"" + id + "\"";
        }
        return String.valueOf(id);
    }
}
