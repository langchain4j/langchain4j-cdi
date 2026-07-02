package dev.langchain4j.cdi.mcp.server.protocol;

/**
 * A JSON-RPC 2.0 notification (a request without an {@code id} that expects no response). Provides factory methods for
 * standard MCP server-to-client notifications.
 */
public class JsonRpcNotification {

    private String jsonrpc = "2.0";
    private String method;

    /** Default constructor for deserialization. */
    public JsonRpcNotification() {}

    /**
     * Creates a notification with the given method name.
     *
     * @param method the JSON-RPC method name
     */
    public JsonRpcNotification(String method) {
        this.method = method;
    }

    private Object params;

    /**
     * Creates a {@code notifications/tools/list_changed} notification.
     *
     * @return a tools-list-changed notification
     */
    public static JsonRpcNotification toolsListChanged() {
        return new JsonRpcNotification("notifications/tools/list_changed");
    }

    /**
     * Creates a {@code notifications/resources/list_changed} notification.
     *
     * @return a resources-list-changed notification
     */
    public static JsonRpcNotification resourcesListChanged() {
        return new JsonRpcNotification("notifications/resources/list_changed");
    }

    /**
     * Creates a {@code notifications/resources/updated} notification for the given resource URI.
     *
     * @param uri the URI of the updated resource
     * @return a resource-updated notification
     */
    public static JsonRpcNotification resourceUpdated(String uri) {
        JsonRpcNotification notification = new JsonRpcNotification("notifications/resources/updated");
        notification.params = java.util.Map.of("uri", uri);
        return notification;
    }

    /**
     * Creates a {@code notifications/prompts/list_changed} notification.
     *
     * @return a prompts-list-changed notification
     */
    public static JsonRpcNotification promptsListChanged() {
        return new JsonRpcNotification("notifications/prompts/list_changed");
    }

    /**
     * Creates a {@code notifications/progress} notification without a message.
     *
     * @param progressToken the token identifying the operation in progress
     * @param progress the current progress value
     * @param total the total progress value (ignored if not positive)
     * @return a progress notification
     */
    public static JsonRpcNotification progress(Object progressToken, double progress, double total) {
        return progress(progressToken, progress, total, null);
    }

    /**
     * Creates a {@code notifications/progress} notification with an optional message.
     *
     * @param progressToken the token identifying the operation in progress
     * @param progress the current progress value
     * @param total the total progress value (ignored if not positive)
     * @param message an optional human-readable progress message, may be {@code null}
     * @return a progress notification
     */
    public static JsonRpcNotification progress(Object progressToken, double progress, double total, String message) {
        JsonRpcNotification notification = new JsonRpcNotification("notifications/progress");
        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("progressToken", progressToken);
        params.put("progress", progress);
        if (total > 0) {
            params.put("total", total);
        }
        if (message != null) {
            params.put("message", message);
        }
        notification.params = params;
        return notification;
    }

    /**
     * Returns the JSON-RPC protocol version.
     *
     * @return the protocol version string, always {@code "2.0"}
     */
    public String getJsonrpc() {
        return jsonrpc;
    }

    /**
     * Sets the JSON-RPC protocol version.
     *
     * @param jsonrpc the protocol version string
     */
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    /**
     * Returns the notification method name.
     *
     * @return the method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the notification method name.
     *
     * @param method the method name
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the notification parameters.
     *
     * @return the parameters, or {@code null} if none
     */
    public Object getParams() {
        return params;
    }

    /**
     * Sets the notification parameters.
     *
     * @param params the parameters
     */
    public void setParams(Object params) {
        this.params = params;
    }
}
