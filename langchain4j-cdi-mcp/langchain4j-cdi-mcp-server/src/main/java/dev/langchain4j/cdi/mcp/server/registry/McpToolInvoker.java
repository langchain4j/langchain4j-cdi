package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;

/** Invokes MCP tool methods on their owning CDI beans, delegating to {@link McpBeanInvoker}. */
@ApplicationScoped
public class McpToolInvoker {

    /** CDI-required default constructor. */
    public McpToolInvoker() {}

    @Inject
    McpBeanInvoker beanInvoker;

    /**
     * Invokes a tool without request context or session.
     *
     * @param requestId the JSON-RPC request ID
     * @param descriptor the tool descriptor identifying the method to call
     * @param arguments the JSON object containing the tool's input arguments
     * @return the tool invocation result
     */
    public Object invoke(Object requestId, McpToolDescriptor descriptor, JsonObject arguments) {
        return beanInvoker.invoke(requestId, descriptor.getBeanType(), descriptor.getMethod(), arguments);
    }

    /**
     * Invokes a tool with request context and session for MCP capability injection.
     *
     * @param requestId the JSON-RPC request ID
     * @param descriptor the tool descriptor identifying the method to call
     * @param arguments the JSON object containing the tool's input arguments
     * @param ctx the MCP request context for progress and cancellation support
     * @param session the MCP session for sampling, elicitation, and roots access
     * @return the tool invocation result
     */
    public Object invoke(
            Object requestId,
            McpToolDescriptor descriptor,
            JsonObject arguments,
            McpRequestContext ctx,
            McpSession session) {
        return beanInvoker.invoke(requestId, descriptor.getBeanType(), descriptor.getMethod(), arguments, ctx, session);
    }
}
