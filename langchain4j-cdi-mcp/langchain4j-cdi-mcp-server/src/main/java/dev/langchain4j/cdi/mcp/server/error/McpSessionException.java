package dev.langchain4j.cdi.mcp.server.error;

public class McpSessionException extends McpException {

    public McpSessionException(Object requestId, String message) {
        super(requestId, McpErrorCode.SESSION_NOT_FOUND, message);
    }
}
