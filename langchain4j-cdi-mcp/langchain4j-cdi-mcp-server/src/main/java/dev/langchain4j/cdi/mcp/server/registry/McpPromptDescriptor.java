package dev.langchain4j.cdi.mcp.server.registry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.prompts.PromptArg;

public class McpPromptDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String name;
    private final String description;
    private final List<PromptArgument> arguments;
    private final Class<?> beanType;
    private final Method method;

    public McpPromptDescriptor(
            String name, String description, List<PromptArgument> arguments, Class<?> beanType, Method method) {
        this.name = name;
        this.description = description;
        this.arguments = arguments;
        this.beanType = beanType;
        this.method = method;
    }

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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<PromptArgument> getArguments() {
        return arguments;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public Method getMethod() {
        return method;
    }

    public record PromptArgument(String name, String description, boolean required) {}
}
