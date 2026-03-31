package dev.langchain4j.cdi.mcp.server.registry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.resources.Resource;
import org.mcp_java.annotations.resources.ResourceTemplate;
import org.mcp_java.annotations.tools.Tool;

@ApplicationScoped
public class McpToolDiscovery {

    private static final Logger LOGGER = Logger.getLogger(McpToolDiscovery.class.getName());

    private final McpToolRegistry toolRegistry;
    private final McpResourceRegistry resourceRegistry;
    private final McpPromptRegistry promptRegistry;
    private final BeanManager beanManager;

    @Inject
    public McpToolDiscovery(
            McpToolRegistry toolRegistry,
            McpResourceRegistry resourceRegistry,
            McpPromptRegistry promptRegistry,
            BeanManager beanManager) {
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.promptRegistry = promptRegistry;
        this.beanManager = beanManager;
    }

    void onStartup(@Observes @Initialized(ApplicationScoped.class) Object event) {
        if (toolRegistry.size() > 0) {
            LOGGER.info(() ->
                    "MCP: Tool registry already populated (" + toolRegistry.size() + " tools), skipping discovery");
            return;
        }
        discoverFromBeanManager();
    }

    private void discoverFromBeanManager() {
        Set<Bean<?>> allBeans = new HashSet<>();
        allBeans.addAll(beanManager.getBeans(Object.class, Any.Literal.INSTANCE));
        allBeans.addAll(beanManager.getBeans(Object.class));

        for (Bean<?> bean : allBeans) {
            Class<?> beanClass = bean.getBeanClass();
            if (beanClass == null) {
                continue;
            }
            Arrays.stream(beanClass.getMethods()).forEach(method -> registerMethod(beanClass, method));
        }

        int total = toolRegistry.size() + resourceRegistry.size() + promptRegistry.size();
        if (total > 0) {
            LOGGER.info(() -> "MCP: Discovered " + toolRegistry.size() + " tool(s), " + resourceRegistry.size()
                    + " resource(s), " + promptRegistry.size() + " prompt(s) via BeanManager scan");
        }
    }

    @SuppressWarnings("java:S1192")
    private void registerMethod(Class<?> beanClass, Method method) {
        if (method.isAnnotationPresent(Tool.class)) {
            McpToolDescriptor descriptor = McpToolDescriptor.fromMethod(beanClass, method);
            toolRegistry.register(descriptor);
            LOGGER.info(() -> "MCP: Registered tool '" + descriptor.getName() + "' from " + beanClass.getSimpleName());
        }
        if (method.isAnnotationPresent(Resource.class)) {
            McpResourceDescriptor descriptor = McpResourceDescriptor.fromMethod(beanClass, method);
            resourceRegistry.register(descriptor);
            LOGGER.info(
                    () -> "MCP: Registered resource '" + descriptor.getUri() + "' from " + beanClass.getSimpleName());
        }
        if (method.isAnnotationPresent(ResourceTemplate.class)) {
            McpResourceTemplateDescriptor descriptor = McpResourceTemplateDescriptor.fromMethod(beanClass, method);
            resourceRegistry.registerTemplate(descriptor);
            LOGGER.info(() -> "MCP: Registered resource template '" + descriptor.getUriTemplate() + "' from "
                    + beanClass.getSimpleName());
        }
        if (method.isAnnotationPresent(Prompt.class)) {
            McpPromptDescriptor descriptor = McpPromptDescriptor.fromMethod(beanClass, method);
            promptRegistry.register(descriptor);
            LOGGER.info(
                    () -> "MCP: Registered prompt '" + descriptor.getName() + "' from " + beanClass.getSimpleName());
        }
    }
}
