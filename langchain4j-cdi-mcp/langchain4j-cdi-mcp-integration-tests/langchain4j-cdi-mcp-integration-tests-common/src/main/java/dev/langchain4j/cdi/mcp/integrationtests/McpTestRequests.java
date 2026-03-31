package dev.langchain4j.cdi.mcp.integrationtests;

public final class McpTestRequests {

    private McpTestRequests() {}

    // --- Lifecycle ---

    public static String initializeRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0\"}}}"
                .formatted(formatId(id));
    }

    @SuppressWarnings("java:S3400")
    public static String initializedNotification() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}";
    }

    public static String pingRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"ping\",\"params\":{}}".formatted(formatId(id));
    }

    // --- Tools ---

    public static String toolsListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"tools/list\",\"params\":{}}".formatted(formatId(id));
    }

    public static String toolsCallRequest(Object id, String toolName, String argsJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"tools/call\",\"params\":{\"name\":\"%s\",\"arguments\":%s}}"
                .formatted(formatId(id), toolName, argsJson);
    }

    // --- Resources ---

    public static String resourcesListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/list\",\"params\":{}}".formatted(formatId(id));
    }

    public static String resourcesReadRequest(Object id, String uri) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/read\",\"params\":{\"uri\":\"%s\"}}"
                .formatted(formatId(id), uri);
    }

    public static String resourcesSubscribeRequest(Object id, String uri) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/subscribe\",\"params\":{\"uri\":\"%s\"}}"
                .formatted(formatId(id), uri);
    }

    public static String resourcesUnsubscribeRequest(Object id, String uri) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/unsubscribe\",\"params\":{\"uri\":\"%s\"}}"
                .formatted(formatId(id), uri);
    }

    public static String resourcesTemplatesListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"resources/templates/list\",\"params\":{}}"
                .formatted(formatId(id));
    }

    // --- Prompts ---

    public static String promptsListRequest(Object id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"prompts/list\",\"params\":{}}".formatted(formatId(id));
    }

    public static String promptsGetRequest(Object id, String name, String argsJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"prompts/get\",\"params\":{\"name\":\"%s\",\"arguments\":%s}}"
                .formatted(formatId(id), name, argsJson);
    }

    public static String promptsGetRequest(Object id, String name) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"prompts/get\",\"params\":{\"name\":\"%s\"}}"
                .formatted(formatId(id), name);
    }

    // --- Completion ---

    public static String completionCompleteRequest(
            Object id, String refType, String refName, String argName, String argValue) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"completion/complete\",\"params\":{\"ref\":{\"type\":\"%s\",\"name\":\"%s\"},\"argument\":{\"name\":\"%s\",\"value\":\"%s\"}}}"
                .formatted(formatId(id), refType, refName, argName, argValue);
    }

    // --- Logging ---

    public static String loggingSetLevelRequest(Object id, String level) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"logging/setLevel\",\"params\":{\"level\":\"%s\"}}"
                .formatted(formatId(id), level);
    }

    // --- Notifications ---

    public static String cancelledNotification(String requestId, String reason) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":\"%s\",\"reason\":\"%s\"}}"
                .formatted(requestId, reason);
    }

    @SuppressWarnings("java:S3400")
    public static String rootsListChangedNotification() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/roots/list_changed\",\"params\":{}}";
    }

    // --- Unknown / Error scenarios ---

    public static String unknownMethodRequest(Object id, String method) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"method\":\"%s\",\"params\":{}}".formatted(formatId(id), method);
    }

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
