package dev.langchain4j.cdi.mcp.server.registry;

import java.lang.reflect.Method;
import org.mcp_java.annotations.resources.Resource;

public class McpResourceDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String uri;
    private final String name;
    private final String description;
    private final String mimeType;
    private final Class<?> beanType;
    private final Method method;

    public McpResourceDescriptor(
            String uri, String name, String description, String mimeType, Class<?> beanType, Method method) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.beanType = beanType;
        this.method = method;
    }

    public static McpResourceDescriptor fromMethod(Class<?> beanClass, Method method) {
        Resource annotation = method.getAnnotation(Resource.class);
        String name = DEFAULT_NAME.equals(annotation.name()) ? method.getName() : annotation.name();
        String mimeType = annotation.mimeType().isEmpty() ? "text/plain" : annotation.mimeType();
        return new McpResourceDescriptor(annotation.uri(), name, annotation.description(), mimeType, beanClass, method);
    }

    public String getUri() {
        return uri;
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
