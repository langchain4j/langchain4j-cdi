package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.resources.Resource;

@ApplicationScoped
public class ConfigResource {

    @Resource(
            uri = "config://app",
            name = "Application Config",
            description = "Current application configuration",
            mimeType = "application/json")
    public String getConfig() {
        return "{\"version\":\"1.0\",\"env\":\"test\"}";
    }

    @Resource(uri = "data://status", name = "Status", description = "Server status")
    public String getStatus() {
        return "running";
    }
}
