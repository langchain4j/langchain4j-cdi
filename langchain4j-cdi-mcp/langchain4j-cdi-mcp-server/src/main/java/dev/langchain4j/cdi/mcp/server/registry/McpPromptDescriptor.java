package dev.langchain4j.cdi.mcp.server.registry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.prompts.PromptArg;

/**
 * Describes an MCP prompt extracted from a {@link Prompt}-annotated method, including its name, description, arguments,
 * owning bean type, and target method.
 */
public class McpPromptDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String name;
    private final String description;
    private final List<PromptArgument> arguments;
    private final Class<?> beanType;
    private final Method method;

    /**
     * Creates a new prompt descriptor.
     *
     * @param name the prompt name
     * @param description the prompt description
     * @param arguments the list of prompt arguments
     * @param beanType the CDI bean class that declares the prompt method
     * @param method the annotated method
     */
    public McpPromptDescriptor(
            String name, String description, List<PromptArgument> arguments, Class<?> beanType, Method method) {
        this.name = name;
        this.description = description;
        this.arguments = arguments;
        this.beanType = beanType;
        this.method = method;
    }

    /**
     * Creates a descriptor by inspecting a {@link Prompt}-annotated method and its parameters.
     *
     * @param beanClass the CDI bean class that declares the method
     * @param method the {@code @Prompt}-annotated method
     * @return a new descriptor built from the method's annotation metadata
     */
    public static McpPromptDescriptor fromMethod(Class<?> beanClass, Method method) {
        Prompt annotation = method.getAnnotation(Prompt.class);
        String name = DEFAULT_NAME.equals(annotation.name()) ? method.getName() : annotation.name();

        List<PromptArgument> args = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            PromptArg argAnnotation = param.getAnnotation(PromptArg.class);
            String argDescription = argAnnotation != null ? argAnnotation.description() : "";
            boolean required = argAnnotation == null || argAnnotation.required();
            args.add(new PromptArgument(param.getName(), argDescription, required));
        }

        return new McpPromptDescriptor(name, annotation.description(), args, beanClass, method);
    }

    /**
     * Returns the prompt name.
     *
     * @return the prompt name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the prompt description.
     *
     * @return the prompt description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the list of prompt arguments.
     *
     * @return the list of prompt arguments
     */
    public List<PromptArgument> getArguments() {
        return arguments;
    }

    /**
     * Returns the CDI bean class that declares the prompt method.
     *
     * @return the bean class
     */
    public Class<?> getBeanType() {
        return beanType;
    }

    /**
     * Returns the annotated method that implements this prompt.
     *
     * @return the prompt method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * A prompt argument descriptor.
     *
     * @param name the argument name
     * @param description the argument description
     * @param required whether the argument is required
     */
    public record PromptArgument(String name, String description, boolean required) {}
}
