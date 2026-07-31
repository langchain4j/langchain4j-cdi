package dev.langchain4j.cdi.mcp.server.fixtures;

import dev.langchain4j.cdi.mcp.server.api.McpLog;
import org.mcpjava.server.progress.Progress;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

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
