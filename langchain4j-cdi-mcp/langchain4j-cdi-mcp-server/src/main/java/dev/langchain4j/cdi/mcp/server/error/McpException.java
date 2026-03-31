package dev.langchain4j.cdi.mcp.server.error;

public class McpException extends RuntimeException {

    private final Object requestId;
    private final McpErrorCode errorCode;

    public McpException(Object requestId, McpErrorCode errorCode, String message) {
        super(message);
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    public Object getRequestId() {
        return requestId;
    }

    public McpErrorCode getErrorCode() {
        return errorCode;
    }
}
