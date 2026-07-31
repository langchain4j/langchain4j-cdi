package dev.langchain4j.cdi.mcp.server.protocol;

/** Server capabilities advertised during MCP initialization. */
public record McpServerCapabilities(
        ToolsCapability tools, ResourcesCapability resources, PromptsCapability prompts, LoggingCapability logging) {

    /** Indicates the server supports tools, optionally with list-change notifications. */
    public record ToolsCapability(boolean listChanged) {}

    /** Indicates the server supports resources, optionally with subscriptions and list-change notifications. */
    public record ResourcesCapability(boolean subscribe, boolean listChanged) {}

    /** Indicates the server supports prompts, optionally with list-change notifications. */
    public record PromptsCapability(boolean listChanged) {}

    /** Indicates the server supports logging. Presence alone signals support; no fields needed. */
    public record LoggingCapability() {
        public static final LoggingCapability INSTANCE = new LoggingCapability();
    }
}
