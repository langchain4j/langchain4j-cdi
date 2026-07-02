package dev.langchain4j.cdi.mcp.server.error;

/** Runtime exception carrying a JSON-RPC error code and the originating request ID. */
public class McpException extends RuntimeException {

    /** The JSON-RPC request ID that triggered the error. */
    private final Object requestId;

    /** The MCP error code describing the failure category. */
    private final McpErrorCode errorCode;

    /**
     * Creates an MCP exception with the given request context and message.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param errorCode the MCP error code describing the failure
     * @param message a human-readable error description
     */
    public McpException(Object requestId, McpErrorCode errorCode, String message) {
        super(message);
        this.requestId = requestId;
        this.errorCode = errorCode;
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
}
