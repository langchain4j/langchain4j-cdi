package dev.langchain4j.cdi.mcp.buildcompatible;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.resources.Resource;
import org.mcp_java.annotations.resources.ResourceTemplate;
import org.mcp_java.annotations.tools.Tool;

public class McpServerBuildCompatibleExtension implements BuildCompatibleExtension {

    private static final Logger LOGGER = Logger.getLogger(McpServerBuildCompatibleExtension.class.getName());
    private static final Set<String> detectedToolBeanClassNames = new HashSet<>();
    private static final Set<String> detectedResourceBeanClassNames = new HashSet<>();
    private static final Set<String> detectedResourceTemplateBeanClassNames = new HashSet<>();
    private static final Set<String> detectedPromptBeanClassNames = new HashSet<>();

    @SuppressWarnings("unused")
    @Enhancement(types = Object.class, withSubtypes = true)
    public void detectMcpBeans(ClassConfig classConfig) {
        try {
            Class<?> clazz = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(classConfig.info().name());

            if (Arrays.stream(clazz.getMethods()).anyMatch(m -> m.isAnnotationPresent(Tool.class))) {
                LOGGER.info(() -> "MCP: Detected @Tool bean: " + clazz.getName());
                detectedToolBeanClassNames.add(clazz.getName());
            }
            if (Arrays.stream(clazz.getMethods()).anyMatch(m -> m.isAnnotationPresent(Resource.class))) {
                LOGGER.info(() -> "MCP: Detected @Resource bean: " + clazz.getName());
                detectedResourceBeanClassNames.add(clazz.getName());
            }
            if (Arrays.stream(clazz.getMethods()).anyMatch(m -> m.isAnnotationPresent(ResourceTemplate.class))) {
                LOGGER.info(() -> "MCP: Detected @ResourceTemplate bean: " + clazz.getName());
                detectedResourceTemplateBeanClassNames.add(clazz.getName());
            }
            if (Arrays.stream(clazz.getMethods()).anyMatch(m -> m.isAnnotationPresent(Prompt.class))) {
                LOGGER.info(() -> "MCP: Detected @Prompt bean: " + clazz.getName());
                detectedPromptBeanClassNames.add(clazz.getName());
            }
        } catch (ClassNotFoundException e) {
            // Ignore classes that can't be loaded
        }
    }

    @SuppressWarnings("unused")
    @Synthesis
    public void registerMcpBeans(SyntheticComponents syntheticComponents) {
        if (detectedToolBeanClassNames.isEmpty()
                && detectedResourceBeanClassNames.isEmpty()
                && detectedResourceTemplateBeanClassNames.isEmpty()
                && detectedPromptBeanClassNames.isEmpty()) {
            LOGGER.info(() -> "MCP: No MCP beans detected during build");
            return;
        }

        LOGGER.info(() -> "MCP: Synthesizing McpRegistryPopulator with " + detectedToolBeanClassNames.size()
                + " tool(s), " + detectedResourceBeanClassNames.size() + " resource(s), "
                + detectedPromptBeanClassNames.size() + " prompt(s)");

        syntheticComponents
                .addBean(McpToolRegistryPopulator.class)
                .type(McpToolRegistryPopulator.class)
                .scope(ApplicationScoped.class)
                .createWith(McpToolRegistryPopulatorCreator.class)
                .withParam(
                        McpToolRegistryPopulatorCreator.PARAM_TOOL_BEAN_CLASSES,
                        detectedToolBeanClassNames.toArray(new String[0]))
                .withParam(
                        McpToolRegistryPopulatorCreator.PARAM_RESOURCE_BEAN_CLASSES,
                        detectedResourceBeanClassNames.toArray(new String[0]))
                .withParam(
                        McpToolRegistryPopulatorCreator.PARAM_RESOURCE_TEMPLATE_BEAN_CLASSES,
                        detectedResourceTemplateBeanClassNames.toArray(new String[0]))
                .withParam(
                        McpToolRegistryPopulatorCreator.PARAM_PROMPT_BEAN_CLASSES,
                        detectedPromptBeanClassNames.toArray(new String[0]));
    }
}
