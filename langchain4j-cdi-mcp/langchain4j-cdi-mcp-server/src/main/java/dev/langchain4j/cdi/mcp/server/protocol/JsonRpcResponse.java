package dev.langchain4j.cdi.mcp.server.protocol;

public class JsonRpcResponse {

    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private JsonRpcError error;

    public JsonRpcResponse() {}

    public static JsonRpcResponse success(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = id;
        response.result = result;
        return response;
    }

    public static JsonRpcResponse error(Object id, JsonRpcError error) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = id;
        response.error = error;
        return response;
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
