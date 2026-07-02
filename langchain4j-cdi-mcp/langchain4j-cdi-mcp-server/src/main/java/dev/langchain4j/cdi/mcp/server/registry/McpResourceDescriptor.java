package dev.langchain4j.cdi.mcp.server.registry;

import java.lang.reflect.Method;
import org.mcp_java.annotations.resources.Resource;

/**
 * Describes an MCP resource extracted from a {@link Resource}-annotated method, including its URI, name, description,
 * MIME type, owning bean type, and target method.
 */
public class McpResourceDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String uri;
    private final String name;
    private final String description;
    private final String mimeType;
    private final Class<?> beanType;
    private final Method method;

    /**
     * Creates a new resource descriptor.
     *
     * @param uri the resource URI
     * @param name the resource name
     * @param description the resource description
     * @param mimeType the MIME type of the resource content
     * @param beanType the CDI bean class that declares the resource method
     * @param method the annotated method
     */
    public McpResourceDescriptor(
            String uri, String name, String description, String mimeType, Class<?> beanType, Method method) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.beanType = beanType;
        this.method = method;
    }

    /**
     * Creates a descriptor by inspecting a {@link Resource}-annotated method.
     *
     * @param beanClass the CDI bean class that declares the method
     * @param method the {@code @Resource}-annotated method
     * @return a new descriptor built from the method's annotation metadata
     */
    public static McpResourceDescriptor fromMethod(Class<?> beanClass, Method method) {
        Resource annotation = method.getAnnotation(Resource.class);
        String name = DEFAULT_NAME.equals(annotation.name()) ? method.getName() : annotation.name();
        String mimeType = annotation.mimeType().isEmpty() ? "text/plain" : annotation.mimeType();
        return new McpResourceDescriptor(annotation.uri(), name, annotation.description(), mimeType, beanClass, method);
    }

    /**
     * Returns the resource URI.
     *
     * @return the resource URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Returns the resource name.
     *
     * @return the resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the resource description.
     *
     * @return the resource description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the MIME type of the resource content.
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the CDI bean class that declares the resource method.
     *
     * @return the bean class
     */
    public Class<?> getBeanType() {
        return beanType;
    }

    /**
     * Returns the annotated method that provides this resource.
     *
     * @return the resource method
     */
    public Method getMethod() {
        return method;
    }
}
