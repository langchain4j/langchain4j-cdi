package dev.langchain4j.cdi.mcp.server.error;

public enum McpErrorCode {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS(-32602),
    INTERNAL_ERROR(-32603),
    SESSION_NOT_FOUND(-32001),
    TOOL_NOT_FOUND(-32002);

    private final int code;

    McpErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
