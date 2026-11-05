package dev.langchain4j.cdi.mcp.server.fixtures;

import org.mcpjava.server.tools.Tool;

/**
 * Test fixture whose tool method always fails, so tests can assert what an invocation failure carries to the error the
 * client receives and to the log the operator reads.
 */
public class FailingTool {

    /** The message of the failure the tool raises. */
    public static final String FAILURE_MESSAGE = "database unreachable";

    /** CDI-required default constructor. */
    public FailingTool() {}

    /**
     * Fails unconditionally.
     *
     * @return never returns
     */
    @Tool(name = "fail", description = "Always fails")
    public String fail() {
        throw new IllegalStateException(FAILURE_MESSAGE);
    }
}
