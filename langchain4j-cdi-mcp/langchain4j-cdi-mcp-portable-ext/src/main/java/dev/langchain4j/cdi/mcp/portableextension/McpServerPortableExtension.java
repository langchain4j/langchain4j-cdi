package dev.langchain4j.cdi.mcp.portableextension;

import dev.langchain4j.cdi.mcp.server.registry.McpPromptDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpPromptRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceTemplateDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpToolDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpToolRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.resources.Resource;
import org.mcp_java.annotations.resources.ResourceTemplate;
import org.mcp_java.annotations.tools.Tool;

public class McpServerPortableExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(McpServerPortableExtension.class.getName());
    private final List<McpToolCandidate> candidates = new ArrayList<>();

    <T> void onProcessManagedBean(@Observes ProcessManagedBean<T> pmb) {
        Class<?> beanClass = pmb.getAnnotatedBeanClass().getJavaClass();

        List<Method> toolMethods = Arrays.stream(beanClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Tool.class))
                .toList();
        List<Method> resourceMethods = Arrays.stream(beanClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Resource.class))
                .toList();
        List<Method> resourceTemplateMethods = Arrays.stream(beanClass.getMethods())
                .filter(m -> m.isAnnotationPresent(ResourceTemplate.class))
                .toList();
        List<Method> promptMethods = Arrays.stream(beanClass.getMethods())
                .filter(m -> m.isAnnotationPresent(Prompt.class))
                .toList();

        if (!toolMethods.isEmpty()
                || !resourceMethods.isEmpty()
                || !resourceTemplateMethods.isEmpty()
                || !promptMethods.isEmpty()) {
            LOGGER.info(() -> "MCP: Detected MCP annotations in " + beanClass.getName());
            candidates.add(new McpToolCandidate(
                    beanClass, toolMethods, resourceMethods, resourceTemplateMethods, promptMethods));
        }
    }

    @SuppressWarnings("java:S1192")
    void onAfterDeployment(@Observes AfterDeploymentValidation adv) {
        if (candidates.isEmpty()) {
            LOGGER.info("MCP: No MCP beans detected");
            return;
        }

        McpToolRegistry toolRegistry =
                CDI.current().select(McpToolRegistry.class).get();
        McpResourceRegistry resourceRegistry =
                CDI.current().select(McpResourceRegistry.class).get();
        McpPromptRegistry promptRegistry =
                CDI.current().select(McpPromptRegistry.class).get();

        for (McpToolCandidate candidate : candidates) {
            for (Method method : candidate.toolMethods()) {
                McpToolDescriptor descriptor = McpToolDescriptor.fromMethod(candidate.beanClass(), method);
                toolRegistry.register(descriptor);
                LOGGER.info(() -> "MCP: Registered tool '" + descriptor.getName() + "' from "
                        + candidate.beanClass().getSimpleName());
            }
            for (Method method : candidate.resourceMethods()) {
                McpResourceDescriptor descriptor = McpResourceDescriptor.fromMethod(candidate.beanClass(), method);
                resourceRegistry.register(descriptor);
                LOGGER.info(() -> "MCP: Registered resource '" + descriptor.getUri() + "' from "
                        + candidate.beanClass().getSimpleName());
            }
            for (Method method : candidate.resourceTemplateMethods()) {
                McpResourceTemplateDescriptor descriptor =
                        McpResourceTemplateDescriptor.fromMethod(candidate.beanClass(), method);
                resourceRegistry.registerTemplate(descriptor);
                LOGGER.info(() -> "MCP: Registered resource template '" + descriptor.getUriTemplate() + "' from "
                        + candidate.beanClass().getSimpleName());
            }
            for (Method method : candidate.promptMethods()) {
                McpPromptDescriptor descriptor = McpPromptDescriptor.fromMethod(candidate.beanClass(), method);
                promptRegistry.register(descriptor);
                LOGGER.info(() -> "MCP: Registered prompt '" + descriptor.getName() + "' from "
                        + candidate.beanClass().getSimpleName());
            }
        }
        LOGGER.info(() -> "MCP: Registered " + toolRegistry.size() + " tool(s), " + resourceRegistry.size()
                + " resource(s), " + promptRegistry.size() + " prompt(s) total");
    }
}
