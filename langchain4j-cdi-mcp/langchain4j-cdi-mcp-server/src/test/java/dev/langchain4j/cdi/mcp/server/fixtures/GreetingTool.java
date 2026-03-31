package dev.langchain4j.cdi.mcp.server.fixtures;

import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;

public class GreetingTool {

    @Tool(description = "Greet someone")
    public String greet(
            @ToolArg(description = "The person's name") String name,
            @ToolArg(description = "Optional greeting style", required = false) String style) {
        if (style != null) {
            return style + ", " + name + "!";
        }
        return "Hello, " + name + "!";
    }
}
