package dev.langchain4j.cdi.mcp.server.registry;

import java.lang.reflect.Method;
import org.mcp_java.annotations.resources.ResourceTemplate;

/**
 * Describes an MCP resource template extracted from a {@link ResourceTemplate}-annotated method, including its URI
 * template, name, description, MIME type, owning bean type, and target method.
 */
public class McpResourceTemplateDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String uriTemplate;
    private final String name;
    private final String description;
    private final String mimeType;
    private final Class<?> beanType;
    private final Method method;

    /**
     * Creates a new resource template descriptor.
     *
     * @param uriTemplate the URI template pattern
     * @param name the resource template name
     * @param description the resource template description
     * @param mimeType the MIME type of the resource content
     * @param beanType the CDI bean class that declares the resource template method
     * @param method the annotated method
     */
    public McpResourceTemplateDescriptor(
            String uriTemplate, String name, String description, String mimeType, Class<?> beanType, Method method) {
        this.uriTemplate = uriTemplate;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.beanType = beanType;
        this.method = method;
    }

    /**
     * Creates a descriptor by inspecting a {@link ResourceTemplate}-annotated method.
     *
     * @param beanClass the CDI bean class that declares the method
     * @param method the {@code @ResourceTemplate}-annotated method
     * @return a new descriptor built from the method's annotation metadata
     */
    public static McpResourceTemplateDescriptor fromMethod(Class<?> beanClass, Method method) {
        ResourceTemplate annotation = method.getAnnotation(ResourceTemplate.class);
        String name = DEFAULT_NAME.equals(annotation.name()) ? method.getName() : annotation.name();
        String mimeType = annotation.mimeType().isEmpty() ? "text/plain" : annotation.mimeType();
        return new McpResourceTemplateDescriptor(
                annotation.uriTemplate(), name, annotation.description(), mimeType, beanClass, method);
    }

    /**
     * Returns the URI template pattern.
     *
     * @return the URI template
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    /**
     * Returns the resource template name.
     *
     * @return the template name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the resource template description.
     *
     * @return the template description
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
     * Returns the CDI bean class that declares the resource template method.
     *
     * @return the bean class
     */
    public Class<?> getBeanType() {
        return beanType;
    }

    /**
     * Returns the annotated method that provides this resource template.
     *
     * @return the resource template method
     */
    public Method getMethod() {
        return method;
    }
}
