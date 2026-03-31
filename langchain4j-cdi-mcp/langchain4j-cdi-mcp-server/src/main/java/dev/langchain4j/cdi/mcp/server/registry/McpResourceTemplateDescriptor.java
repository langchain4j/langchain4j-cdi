package dev.langchain4j.cdi.mcp.server.registry;

import java.lang.reflect.Method;
import org.mcp_java.annotations.resources.ResourceTemplate;

public class McpResourceTemplateDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String uriTemplate;
    private final String name;
    private final String description;
    private final String mimeType;
    private final Class<?> beanType;
    private final Method method;

    public McpResourceTemplateDescriptor(
            String uriTemplate, String name, String description, String mimeType, Class<?> beanType, Method method) {
        this.uriTemplate = uriTemplate;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.beanType = beanType;
        this.method = method;
    }

    public static McpResourceTemplateDescriptor fromMethod(Class<?> beanClass, Method method) {
        ResourceTemplate annotation = method.getAnnotation(ResourceTemplate.class);
        String name = DEFAULT_NAME.equals(annotation.name()) ? method.getName() : annotation.name();
        String mimeType = annotation.mimeType().isEmpty() ? "text/plain" : annotation.mimeType();
        return new McpResourceTemplateDescriptor(
                annotation.uriTemplate(), name, annotation.description(), mimeType, beanClass, method);
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public Method getMethod() {
        return method;
    }
}
