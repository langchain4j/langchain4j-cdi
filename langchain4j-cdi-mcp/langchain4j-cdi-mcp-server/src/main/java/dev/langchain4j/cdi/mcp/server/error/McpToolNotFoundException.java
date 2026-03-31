package dev.langchain4j.cdi.mcp.server.error;

public class McpToolNotFoundException extends McpException {

    public McpToolNotFoundException(Object requestId, String toolName) {
        super(requestId, McpErrorCode.TOOL_NOT_FOUND, "Tool not found: " + toolName);
    }
}
