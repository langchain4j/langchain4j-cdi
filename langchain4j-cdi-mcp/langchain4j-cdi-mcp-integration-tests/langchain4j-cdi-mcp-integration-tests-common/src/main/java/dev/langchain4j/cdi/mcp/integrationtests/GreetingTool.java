package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;

@ApplicationScoped
public class GreetingTool {

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
