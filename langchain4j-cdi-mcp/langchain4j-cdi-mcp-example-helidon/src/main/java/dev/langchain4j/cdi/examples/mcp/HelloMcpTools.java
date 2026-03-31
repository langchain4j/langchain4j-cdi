package dev.langchain4j.cdi.examples.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.prompts.PromptArg;
import org.mcp_java.annotations.resources.Resource;
import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;

@SuppressWarnings("unused")
@ApplicationScoped
public class HelloMcpTools {

    @Tool(description = "Say hello to someone by name")
    public String hello(@ToolArg(description = "The name of the person to greet") String name) {
        return "Hello, " + name + "!";
    }

    @Resource(
            uri = "config://server",
            name = "Server Config",
            description = "Current server configuration",
            mimeType = "application/json")
    public String serverConfig() {
        return "{\"name\":\"helidon-mcp-server\",\"version\":\"1.0\"}";
    }

    @Prompt(description = "Generate a greeting message for a person")
    public String greetingPrompt(@PromptArg(description = "The person's name") String name) {
        return "Write a warm and friendly greeting for " + name + ". Be creative and personal.";
    }
}
