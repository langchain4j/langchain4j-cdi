package dev.langchain4j.cdi.mcp.server.protocol;

public class JsonRpcError {

    private int code;
    private String message;

    public JsonRpcError() {}

    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
