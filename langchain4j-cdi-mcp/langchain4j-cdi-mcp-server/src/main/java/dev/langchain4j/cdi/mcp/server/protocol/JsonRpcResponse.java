package dev.langchain4j.cdi.mcp.server.protocol;

/**
 * A JSON-RPC 2.0 response. Contains either a {@code result} (on success) or an {@code error} (on failure), correlated
 * to a request by {@code id}.
 */
public class JsonRpcResponse {

    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private JsonRpcError error;

    /** Default constructor for deserialization. */
    public JsonRpcResponse() {}

    /**
     * Creates a successful response with the given id and result.
     *
     * @param id the request identifier this response corresponds to
     * @param result the result payload
     * @return a success response
     */
    public static JsonRpcResponse success(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = id;
        response.result = result;
        return response;
    }

    /**
     * Creates an error response with the given id and error details.
     *
     * @param id the request identifier this response corresponds to
     * @param error the error details
     * @return an error response
     */
    public static JsonRpcResponse error(Object id, JsonRpcError error) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = id;
        response.error = error;
        return response;
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
     * Returns the request identifier this response corresponds to.
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
     * Returns the result payload on success.
     *
     * @return the result, or {@code null} if this is an error response
     */
    public Object getResult() {
        return result;
    }

    /**
     * Sets the result payload.
     *
     * @param result the result
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * Returns the error details on failure.
     *
     * @return the error, or {@code null} if this is a success response
     */
    public JsonRpcError getError() {
        return error;
    }

    /**
     * Sets the error details.
     *
     * @param error the error
     */
    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
