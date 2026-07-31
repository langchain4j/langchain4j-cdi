package dev.langchain4j.cdi.examples.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcpjava.server.prompts.Prompt;
import org.mcpjava.server.prompts.PromptArg;
import org.mcpjava.server.resources.Resource;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

/** Example CDI bean exposing MCP tools, resources, and prompts for the Helidon MCP server demo. */
@SuppressWarnings("unused")
@ApplicationScoped
public class HelloMcpTools {

    /** Creates a new HelloMcpTools. */
    public HelloMcpTools() {}

    /**
     * Greets a person by name.
     *
     * @param name the name of the person to greet
     * @return a greeting message
     */
    @Tool(description = "Say hello to someone by name")
    public String hello(@ToolArg(description = "The name of the person to greet") String name) {
        return "Hello, " + name + "!";
    }

    /**
     * Returns the current server configuration as JSON.
     *
     * @return the server configuration in JSON format
     */
    @Resource(
            uri = "config://server",
            name = "Server Config",
            description = "Current server configuration",
            mimeType = "application/json")
    public String serverConfig() {
        return "{\"name\":\"helidon-mcp-server\",\"version\":\"1.0\"}";
    }

    /**
     * Generates a greeting prompt for a person.
     *
     * @param name the name of the person to greet
     * @return a prompt string requesting a creative greeting
     */
    @Prompt(description = "Generate a greeting message for a person")
    public String greetingPrompt(@PromptArg(description = "The person's name") String name) {
        return "Write a warm and friendly greeting for " + name + ". Be creative and personal.";
    }
}
