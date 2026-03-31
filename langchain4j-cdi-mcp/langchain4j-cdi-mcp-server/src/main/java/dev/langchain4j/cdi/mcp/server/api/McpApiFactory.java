package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.logging.McpLogger;
import dev.langchain4j.cdi.mcp.server.transport.McpElicitationManager;
import dev.langchain4j.cdi.mcp.server.transport.McpProgressReporter;
import dev.langchain4j.cdi.mcp.server.transport.McpRootsManager;
import dev.langchain4j.cdi.mcp.server.transport.McpSamplingManager;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mcp_java.server.Cancellation;
import org.mcp_java.server.Elicitation;
import org.mcp_java.server.McpConnection;
import org.mcp_java.server.McpLog;
import org.mcp_java.server.Progress;
import org.mcp_java.server.Roots;
import org.mcp_java.server.Sampling;

/**
 * Factory that creates MCP framework type instances from request context. Used by {@code McpBeanInvoker} to inject
 * framework types into {@code @Tool}, {@code @Prompt}, and {@code @Resource} method parameters.
 */
@ApplicationScoped
public class McpApiFactory {

    @Inject
    McpLogger mcpLogger;

    @Inject
    McpProgressReporter progressReporter;

    @Inject
    McpRootsManager rootsManager;

    @Inject
    McpSamplingManager samplingManager;

    @Inject
    McpElicitationManager elicitationManager;

    /**
     * Creates an instance of the given MCP framework type.
     *
     * @param type the framework interface type (McpLog, Progress, etc.)
     * @param ctx the per-request context
     * @param session the MCP session
     * @param beanType the CDI bean type being invoked (used for logger naming)
     * @return the framework type instance
     * @throws IllegalArgumentException if the type is not a known framework type
     */
    public Object createInstance(Class<?> type, McpRequestContext ctx, McpSession session, Class<?> beanType) {
        if (type == McpLog.class) {
            return new CdiMcpLog(mcpLogger, beanType.getSimpleName());
        }
        if (type == Progress.class) {
            return new CdiProgress(ctx != null ? ctx.progressToken() : null, progressReporter);
        }
        if (type == Cancellation.class) {
            return new CdiCancellation(ctx != null ? ctx.cancelledFlag() : null);
        }
        if (type == McpConnection.class) {
            return new CdiMcpConnection(session, mcpLogger);
        }
        if (type == Roots.class) {
            String sessionId = ctx != null ? ctx.sessionId() : session.getId();
            return new CdiRoots(session, rootsManager, sessionId);
        }
        if (type == Sampling.class) {
            String sessionId = ctx != null ? ctx.sessionId() : session.getId();
            return new CdiSampling(session, samplingManager, sessionId);
        }
        if (type == Elicitation.class) {
            String sessionId = ctx != null ? ctx.sessionId() : session.getId();
            return new CdiElicitation(session, elicitationManager, sessionId);
        }
        throw new IllegalArgumentException("Unknown MCP framework type: " + type.getName());
    }
}
