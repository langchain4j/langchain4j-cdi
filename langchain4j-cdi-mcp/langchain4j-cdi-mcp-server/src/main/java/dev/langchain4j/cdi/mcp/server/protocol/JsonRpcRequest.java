package dev.langchain4j.cdi.mcp.server.protocol;

import jakarta.json.JsonObject;

public class JsonRpcRequest {

    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private JsonObject params;
    private Object progressToken;

    public JsonRpcRequest() {}

    public JsonRpcRequest(Object id, String method, JsonObject params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonObject getParams() {
        return params;
    }

    public void setParams(JsonObject params) {
        this.params = params;
    }

    public Object getProgressToken() {
        return progressToken;
    }

    public void setProgressToken(Object progressToken) {
        this.progressToken = progressToken;
    }
}
