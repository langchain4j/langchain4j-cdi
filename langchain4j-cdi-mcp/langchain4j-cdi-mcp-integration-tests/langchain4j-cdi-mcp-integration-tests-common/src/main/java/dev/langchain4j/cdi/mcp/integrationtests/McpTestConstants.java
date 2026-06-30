package dev.langchain4j.cdi.mcp.integrationtests;

/** Shared constants for MCP integration tests. */
public final class McpTestConstants {

    /** Tool name for the weather tool. */
    public static final String GET_WEATHER = "getWeather";

    /** Tool name for the greeting tool. */
    public static final String GREET = "greet";

    /** HTTP header name for the MCP session identifier. */
    public static final String MCP_SESSION_ID = "Mcp-Session-Id";

    /** URI of the application configuration resource. */
    public static final String CONFIG_APP = "config://app";

    /** JSON key for the resources array. */
    public static final String RESOURCES = "resources";

    /** Prompt name for the summarize prompt. */
    public static final String SUMMARIZE = "summarize";

    private McpTestConstants() {}
}
