package dev.langchain4j.cdi.mcp.server.protocol;

/**
 * A JSON-RPC 2.0 request sent from the server to the client. Used for server-initiated operations like
 * {@code roots/list} and {@code sampling/createMessage}.
 */
public class JsonRpcServerRequest {

    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private Object params;

    public JsonRpcServerRequest() {}

    public JsonRpcServerRequest(Object id, String method, Object params) {
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

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
