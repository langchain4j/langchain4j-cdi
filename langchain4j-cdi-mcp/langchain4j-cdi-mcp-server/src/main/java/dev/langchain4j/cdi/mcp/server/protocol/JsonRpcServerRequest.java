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

    /** Default constructor for deserialization. */
    public JsonRpcServerRequest() {}

    /**
     * Creates a server request with the given id, method, and parameters.
     *
     * @param id the request identifier for correlating the client response
     * @param method the JSON-RPC method name
     * @param params the method parameters
     */
    public JsonRpcServerRequest(Object id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
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
     * Returns the request identifier for correlating the client response.
     *
     * @return the request id
     */
    public Object getId() {
        return id;
    }

    /**
     * Sets the request identifier.
     *
     * @param id the request id
     */
    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Returns the JSON-RPC method name.
     *
     * @return the method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the JSON-RPC method name.
     *
     * @param method the method name
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the method parameters.
     *
     * @return the parameters, or {@code null} if none
     */
    public Object getParams() {
        return params;
    }

    /**
     * Sets the method parameters.
     *
     * @param params the parameters
     */
    public void setParams(Object params) {
        this.params = params;
    }
}
