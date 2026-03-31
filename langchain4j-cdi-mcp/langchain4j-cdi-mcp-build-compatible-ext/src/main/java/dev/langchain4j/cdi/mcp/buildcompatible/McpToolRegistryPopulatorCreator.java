package dev.langchain4j.cdi.mcp.buildcompatible;

import dev.langchain4j.cdi.mcp.server.registry.McpPromptDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpPromptRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceRegistry;
import dev.langchain4j.cdi.mcp.server.registry.McpResourceTemplateDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpToolDescriptor;
import dev.langchain4j.cdi.mcp.server.registry.McpToolRegistry;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import java.util.Arrays;
import java.util.logging.Logger;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.resources.Resource;
import org.mcp_java.annotations.resources.ResourceTemplate;
import org.mcp_java.annotations.tools.Tool;

public class McpToolRegistryPopulatorCreator implements SyntheticBeanCreator<McpToolRegistryPopulator> {

    private static final Logger LOGGER = Logger.getLogger(McpToolRegistryPopulatorCreator.class.getName());
    public static final String PARAM_TOOL_BEAN_CLASSES = "toolBeanClasses";
    public static final String PARAM_RESOURCE_BEAN_CLASSES = "resourceBeanClasses";
    public static final String PARAM_RESOURCE_TEMPLATE_BEAN_CLASSES = "resourceTemplateBeanClasses";
    public static final String PARAM_PROMPT_BEAN_CLASSES = "promptBeanClasses";

    @Override
    @SuppressWarnings("java:S1192")
    public McpToolRegistryPopulator create(jakarta.enterprise.inject.Instance<Object> lookup, Parameters params) {
        McpToolRegistry toolRegistry = lookup.select(McpToolRegistry.class).get();
        McpResourceRegistry resourceRegistry =
                lookup.select(McpResourceRegistry.class).get();
        McpPromptRegistry promptRegistry =
                lookup.select(McpPromptRegistry.class).get();

        registerBeans(params, PARAM_TOOL_BEAN_CLASSES, Tool.class, (beanClass, method) -> {
            McpToolDescriptor descriptor = McpToolDescriptor.fromMethod(beanClass, method);
            toolRegistry.register(descriptor);
            LOGGER.info(() -> "MCP: Registered tool '" + descriptor.getName() + "' from " + beanClass.getSimpleName());
        });

        registerBeans(params, PARAM_RESOURCE_BEAN_CLASSES, Resource.class, (beanClass, method) -> {
            McpResourceDescriptor descriptor = McpResourceDescriptor.fromMethod(beanClass, method);
            resourceRegistry.register(descriptor);
            LOGGER.info(
                    () -> "MCP: Registered resource '" + descriptor.getUri() + "' from " + beanClass.getSimpleName());
        });

        registerBeans(params, PARAM_RESOURCE_TEMPLATE_BEAN_CLASSES, ResourceTemplate.class, (beanClass, method) -> {
            McpResourceTemplateDescriptor descriptor = McpResourceTemplateDescriptor.fromMethod(beanClass, method);
            resourceRegistry.registerTemplate(descriptor);
            LOGGER.info(() -> "MCP: Registered resource template '" + descriptor.getUriTemplate() + "' from "
                    + beanClass.getSimpleName());
        });

        registerBeans(params, PARAM_PROMPT_BEAN_CLASSES, Prompt.class, (beanClass, method) -> {
            McpPromptDescriptor descriptor = McpPromptDescriptor.fromMethod(beanClass, method);
            promptRegistry.register(descriptor);
            LOGGER.info(
                    () -> "MCP: Registered prompt '" + descriptor.getName() + "' from " + beanClass.getSimpleName());
        });

        LOGGER.info(() -> "MCP: Registered " + toolRegistry.size() + " tool(s), " + resourceRegistry.size()
                + " resource(s), " + promptRegistry.size() + " prompt(s) via build-compatible extension");

        return new McpToolRegistryPopulator();
    }

    private void registerBeans(
            Parameters params,
            String paramName,
            Class<? extends java.lang.annotation.Annotation> annotation,
            MethodRegistrar registrar) {
        String[] classNames = params.get(paramName, String[].class);
        if (classNames == null) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> beanClass =
                        Thread.currentThread().getContextClassLoader().loadClass(className);
                Arrays.stream(beanClass.getMethods())
                        .filter(m -> m.isAnnotationPresent(annotation))
                        .forEach(method -> registrar.register(beanClass, method));
            } catch (ClassNotFoundException e) {
                LOGGER.warning(() -> "MCP: Could not load bean class: " + className);
            }
        }
    }

    @FunctionalInterface
    private interface MethodRegistrar {
        void register(Class<?> beanClass, java.lang.reflect.Method method);
    }
}
