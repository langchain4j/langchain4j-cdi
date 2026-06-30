package dev.langchain4j.cdi.mcp.server.error;

/** Exception thrown when a requested MCP tool cannot be found in the registry. */
public class McpToolNotFoundException extends McpException {

    /**
     * Creates a tool-not-found exception for the given tool name.
     *
     * @param requestId the JSON-RPC request ID that triggered the error
     * @param toolName the name of the tool that was not found
     */
    public McpToolNotFoundException(Object requestId, String toolName) {
        super(requestId, McpErrorCode.TOOL_NOT_FOUND, "Tool not found: " + toolName);
    }
}
