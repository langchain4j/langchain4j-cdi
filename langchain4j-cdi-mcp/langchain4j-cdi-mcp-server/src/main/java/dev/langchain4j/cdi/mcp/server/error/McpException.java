package dev.langchain4j.cdi.mcp.server.error;

/** Runtime exception carrying a JSON-RPC error code and the originating request ID. */
public class McpException extends RuntimeException {

    /** The JSON-RPC request ID that triggered the error. */
    private final Object requestId;

    /** The MCP error code describing the failure category. */
    private final McpErrorCode errorCode;

    /** The HTTP status to use when this error is returned over Streamable HTTP. */
    private final int httpStatus;

    /** Optional JSON-RPC error {@code data}, or {@code null}. */
    private final Object data;

    /**
     * Creates an MCP exception with the given request context and message.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param errorCode the MCP error code describing the failure
     * @param message a human-readable error description
     */
    public McpException(Object requestId, McpErrorCode errorCode, String message) {
        this(requestId, errorCode, message, 200, null, null);
    }

    /**
     * Creates an MCP exception with the given request context and message, chaining the failure that caused it.
     *
     * <p>The cause is never sent to the client: the JSON-RPC error carries only the message and {@code data}. Chaining
     * it keeps the original exception — the one an operator has to see in the server log — reachable from the error
     * that travels up the call chain.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param errorCode the MCP error code describing the failure
     * @param message a human-readable error description
     * @param cause the failure that led to this error, or {@code null}
     */
    public McpException(Object requestId, McpErrorCode errorCode, String message, Throwable cause) {
        this(requestId, errorCode, message, 200, null, cause);
    }

    /**
     * Creates an MCP exception with request context, error code, message, HTTP status, and optional error data.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param errorCode the MCP error code describing the failure
     * @param message a human-readable error description
     * @param httpStatus the HTTP status to use when this error is returned over Streamable HTTP
     * @param data optional JSON-RPC error {@code data}, or {@code null}
     */
    public McpException(Object requestId, McpErrorCode errorCode, String message, int httpStatus, Object data) {
        this(requestId, errorCode, message, httpStatus, data, null);
    }

    /**
     * Creates an MCP exception with request context, error code, message, HTTP status, optional error data, and the
     * failure that caused it.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param errorCode the MCP error code describing the failure
     * @param message a human-readable error description
     * @param httpStatus the HTTP status to use when this error is returned over Streamable HTTP
     * @param data optional JSON-RPC error {@code data}, or {@code null}
     * @param cause the failure that led to this error, or {@code null}
     */
    public McpException(
            Object requestId, McpErrorCode errorCode, String message, int httpStatus, Object data, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.data = data;
    }

    /**
     * Returns the JSON-RPC request ID associated with this error.
     *
     * @return the request ID
     */
    public Object getRequestId() {
        return requestId;
    }

    /**
     * Returns the MCP error code describing the failure.
     *
     * @return the error code
     */
    public McpErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the HTTP status to use when this error is returned over Streamable HTTP.
     *
     * @return the HTTP status
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Returns the optional JSON-RPC error {@code data}, or {@code null}.
     *
     * @return the error data, or {@code null}
     */
    public Object getData() {
        return data;
    }
}
