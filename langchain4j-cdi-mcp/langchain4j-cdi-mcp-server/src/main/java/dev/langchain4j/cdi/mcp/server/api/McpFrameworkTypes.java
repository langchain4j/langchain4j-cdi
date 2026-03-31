package dev.langchain4j.cdi.mcp.server.api;

import java.util.Set;
import org.mcp_java.server.Cancellation;
import org.mcp_java.server.Elicitation;
import org.mcp_java.server.McpConnection;
import org.mcp_java.server.McpLog;
import org.mcp_java.server.Progress;
import org.mcp_java.server.Roots;
import org.mcp_java.server.Sampling;

/**
 * Registry of MCP framework types that can be injected as parameters in {@code @Tool}, {@code @Prompt}, and
 * {@code @Resource} methods. These types are excluded from JSON schema generation and resolved by the framework at
 * invocation time.
 */
public final class McpFrameworkTypes {

    private static final Set<Class<?>> FRAMEWORK_TYPES = Set.of(
            McpLog.class,
            McpConnection.class,
            Progress.class,
            Cancellation.class,
            Roots.class,
            Sampling.class,
            Elicitation.class);

    private McpFrameworkTypes() {}

    public static boolean isFrameworkType(Class<?> type) {
        return FRAMEWORK_TYPES.contains(type);
    }
}
