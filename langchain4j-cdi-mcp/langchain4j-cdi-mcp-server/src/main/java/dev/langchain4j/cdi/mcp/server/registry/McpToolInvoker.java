package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;

@ApplicationScoped
public class McpToolInvoker {

    @Inject
    McpBeanInvoker beanInvoker;

    public Object invoke(Object requestId, McpToolDescriptor descriptor, JsonObject arguments) {
        return beanInvoker.invoke(requestId, descriptor.getBeanType(), descriptor.getMethod(), arguments);
    }

    public Object invoke(
            Object requestId,
            McpToolDescriptor descriptor,
            JsonObject arguments,
            McpRequestContext ctx,
            McpSession session) {
        return beanInvoker.invoke(requestId, descriptor.getBeanType(), descriptor.getMethod(), arguments, ctx, session);
    }
}
