package dev.langchain4j.cdi.mcp.server.protocol;

import jakarta.json.JsonObject;

/**
 * A JSON-RPC 2.0 request received from a client. Contains the method to invoke, parameters, and a unique request
 * identifier for correlating the response.
 */
public class JsonRpcRequest {

    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private JsonObject params;
    private Object progressToken;

    /** Default constructor for deserialization. */
    public JsonRpcRequest() {}

    /**
     * Creates a request with the given id, method, and parameters.
     *
     * @param id the request identifier
     * @param method the JSON-RPC method name
     * @param params the method parameters
     */
    public JsonRpcRequest(Object id, String method, JsonObject params) {
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
     * Returns the request identifier used to correlate the response.
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
     * Returns the JSON-RPC method name to invoke.
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
     * @return the parameters as a JSON object, or {@code null} if none
     */
    public JsonObject getParams() {
        return params;
    }

    /**
     * Sets the method parameters.
     *
     * @param params the parameters as a JSON object
     */
    public void setParams(JsonObject params) {
        this.params = params;
    }

    /**
     * Returns the progress token for tracking long-running operations.
     *
     * @return the progress token, or {@code null} if not set
     */
    public Object getProgressToken() {
        return progressToken;
    }

    /**
     * Sets the progress token for tracking long-running operations.
     *
     * @param progressToken the progress token
     */
    public void setProgressToken(Object progressToken) {
        this.progressToken = progressToken;
    }
}
