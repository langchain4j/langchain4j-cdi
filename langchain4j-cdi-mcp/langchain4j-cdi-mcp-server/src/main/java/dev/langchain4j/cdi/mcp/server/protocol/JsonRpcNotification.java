package dev.langchain4j.cdi.mcp.server.protocol;

public class JsonRpcNotification {

    private String jsonrpc = "2.0";
    private String method;

    public JsonRpcNotification() {}

    public JsonRpcNotification(String method) {
        this.method = method;
    }

    private Object params;

    public static JsonRpcNotification toolsListChanged() {
        return new JsonRpcNotification("notifications/tools/list_changed");
    }

    public static JsonRpcNotification resourcesListChanged() {
        return new JsonRpcNotification("notifications/resources/list_changed");
    }

    public static JsonRpcNotification resourceUpdated(String uri) {
        JsonRpcNotification notification = new JsonRpcNotification("notifications/resources/updated");
        notification.params = java.util.Map.of("uri", uri);
        return notification;
    }

    public static JsonRpcNotification promptsListChanged() {
        return new JsonRpcNotification("notifications/prompts/list_changed");
    }

    public static JsonRpcNotification progress(Object progressToken, double progress, double total) {
        return progress(progressToken, progress, total, null);
    }

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

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
