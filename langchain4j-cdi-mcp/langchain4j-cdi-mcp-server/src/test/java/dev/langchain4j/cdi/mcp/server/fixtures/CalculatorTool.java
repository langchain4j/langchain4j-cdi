package dev.langchain4j.cdi.mcp.server.fixtures;

import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;

public class CalculatorTool {

    @Tool(description = "Add two numbers together")
    public int add(@ToolArg(description = "First number") int a, @ToolArg(description = "Second number") int b) {
        return a + b;
    }

    @Tool(description = "Multiply two numbers")
    public double multiply(
            @ToolArg(description = "First number") double a, @ToolArg(description = "Second number") double b) {
        return a * b;
    }
}
