package dev.langchain4j.cdi.mcp.server.error;

/** JSON-RPC 2.0 error codes used in MCP protocol responses. */
public enum McpErrorCode {
    /** The request payload could not be parsed as valid JSON. */
    PARSE_ERROR(-32700),
    /** The request is not a valid JSON-RPC 2.0 request. */
    INVALID_REQUEST(-32600),
    /** The requested method does not exist or is not available. */
    METHOD_NOT_FOUND(-32601),
    /** Invalid method parameters were supplied. */
    INVALID_PARAMS(-32602),
    /** An internal server error occurred during processing. */
    INTERNAL_ERROR(-32603),
    /** The referenced MCP session was not found or has expired. */
    SESSION_NOT_FOUND(-32001),
    /** The requested tool was not found in the registry. */
    TOOL_NOT_FOUND(-32002);

    private final int code;

    McpErrorCode(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric JSON-RPC error code.
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }
}
