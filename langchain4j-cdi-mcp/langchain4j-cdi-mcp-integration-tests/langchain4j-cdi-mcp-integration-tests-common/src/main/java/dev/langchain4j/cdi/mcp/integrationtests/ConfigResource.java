package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.resources.Resource;

/** MCP resource provider exposing application configuration and status. */
@ApplicationScoped
public class ConfigResource {

    /** Creates a new instance. */
    public ConfigResource() {}

    /**
     * Returns the application configuration as JSON.
     *
     * @return the configuration JSON string
     */
    @Resource(
            uri = "config://app",
            name = "Application Config",
            description = "Current application configuration",
            mimeType = "application/json")
    public String getConfig() {
        return "{\"version\":\"1.0\",\"env\":\"test\"}";
    }

    /**
     * Returns the server status.
     *
     * @return the status string
     */
    @Resource(uri = "data://status", name = "Status", description = "Server status")
    public String getStatus() {
        return "running";
    }
}
