package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.schema.JsonSchemaGenerator;
import jakarta.json.JsonObject;
import java.lang.reflect.Method;
import org.mcp_java.annotations.tools.Tool;

/** Describes a discovered MCP tool, holding its metadata, JSON Schema, and the backing bean method. */
public class McpToolDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String name;
    private final String description;
    private final JsonObject inputSchema;
    private final Class<?> beanType;
    private final Method method;

    /**
     * Creates a tool descriptor with the given metadata.
     *
     * @param name the tool name
     * @param description a human-readable description of the tool
     * @param inputSchema the JSON Schema describing the tool's input parameters
     * @param beanType the CDI bean class that declares the tool method
     * @param method the reflective method reference to invoke
     */
    public McpToolDescriptor(
            String name, String description, JsonObject inputSchema, Class<?> beanType, Method method) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.beanType = beanType;
        this.method = method;
    }

    /**
     * Creates a descriptor by inspecting a {@link Tool}-annotated method.
     *
     * @param beanClass the CDI bean class that declares the method
     * @param method the method annotated with {@link Tool}
     * @return a new descriptor populated from the annotation and method signature
     */
    public static McpToolDescriptor fromMethod(Class<?> beanClass, Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        String toolName = DEFAULT_NAME.equals(tool.name()) ? method.getName() : tool.name();
        String toolDescription = tool.description();
        return new McpToolDescriptor(
                toolName, toolDescription, JsonSchemaGenerator.fromMethod(method), beanClass, method);
    }

    /**
     * Converts this descriptor to the MCP wire-format tool representation.
     *
     * @return an MCP {@link org.mcp_java.model.tool.Tool} suitable for JSON serialization
     */
    public org.mcp_java.model.tool.Tool toWireFormat() {
        return new org.mcp_java.model.tool.Tool(name, null, description, inputSchema, null, null, null);
    }

    /**
     * Returns the tool name.
     *
     * @return the tool name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the human-readable tool description.
     *
     * @return the tool description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the JSON Schema describing the tool's input parameters.
     *
     * @return the input schema as a JSON object
     */
    public JsonObject getInputSchema() {
        return inputSchema;
    }

    /**
     * Returns the CDI bean class that declares the tool method.
     *
     * @return the bean class
     */
    public Class<?> getBeanType() {
        return beanType;
    }

    /**
     * Returns the reflective method reference for invoking the tool.
     *
     * @return the tool method
     */
    public Method getMethod() {
        return method;
    }
}
