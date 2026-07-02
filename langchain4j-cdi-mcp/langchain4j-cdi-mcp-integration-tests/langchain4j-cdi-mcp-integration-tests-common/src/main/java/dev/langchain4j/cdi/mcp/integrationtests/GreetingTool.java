package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;

/** MCP tool that generates greeting messages. */
@ApplicationScoped
public class GreetingTool {

    /** Creates a new instance. */
    public GreetingTool() {}

    /**
     * Greets someone by name with an optional prefix.
     *
     * @param name the person's name
     * @param prefix optional greeting prefix, defaults to "Hello"
     * @return the greeting message
     */
    @Tool(description = "Greet someone by name")
    public String greet(
            @ToolArg(description = "The person's name") String name,
            @ToolArg(description = "Optional greeting prefix", required = false) String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + ", " + name + "!";
        }
        return "Hello, " + name + "!";
    }
}
