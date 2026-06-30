package dev.langchain4j.cdi.mcp.server.protocol;

/** Represents a JSON-RPC 2.0 error object. */
public class JsonRpcError {

    private int code;
    private String message;

    /** Default constructor for JSON-B deserialization. */
    public JsonRpcError() {}

    /**
     * Creates an error with the given code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the error code.
     *
     * @param code the error code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message.
     *
     * @param message the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
