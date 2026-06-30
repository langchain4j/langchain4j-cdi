package dev.langchain4j.cdi.mcp.server.error;

/** Exception thrown when an MCP session cannot be found or has expired. */
public class McpSessionException extends McpException {

    /**
     * Creates a session exception with the given request ID and message.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param message a human-readable description of the session error
     */
    public McpSessionException(Object requestId, String message) {
        super(requestId, McpErrorCode.SESSION_NOT_FOUND, message);
    }
}
