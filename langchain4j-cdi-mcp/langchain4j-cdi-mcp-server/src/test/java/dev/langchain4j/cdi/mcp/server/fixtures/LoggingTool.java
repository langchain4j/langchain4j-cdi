package dev.langchain4j.cdi.mcp.server.fixtures;

import org.mcp_java.annotations.tools.Tool;
import org.mcp_java.annotations.tools.ToolArg;
import org.mcp_java.server.McpLog;
import org.mcp_java.server.Progress;

public class LoggingTool {

    @Tool(description = "A tool that logs and reports progress")
    public String doWork(@ToolArg(description = "The input text") String input, McpLog log, Progress progress) {
        log.info("Processing: {}", input);
        return "Done: " + input;
    }

    @Tool(description = "Simple tool without framework types")
    public String simpleTool(@ToolArg(description = "The name") String name) {
        return "Hello " + name;
    }
}
