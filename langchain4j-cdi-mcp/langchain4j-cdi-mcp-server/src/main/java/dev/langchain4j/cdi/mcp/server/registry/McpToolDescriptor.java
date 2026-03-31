package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.schema.JsonSchemaGenerator;
import jakarta.json.JsonObject;
import java.lang.reflect.Method;
import org.mcp_java.annotations.tools.Tool;

public class McpToolDescriptor {

    private static final String DEFAULT_NAME = "<<element name>>";

    private final String name;
    private final String description;
    private final JsonObject inputSchema;
    private final Class<?> beanType;
    private final Method method;

    public McpToolDescriptor(
            String name, String description, JsonObject inputSchema, Class<?> beanType, Method method) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.beanType = beanType;
        this.method = method;
    }

    public static McpToolDescriptor fromMethod(Class<?> beanClass, Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        String toolName = DEFAULT_NAME.equals(tool.name()) ? method.getName() : tool.name();
        String toolDescription = tool.description();
        return new McpToolDescriptor(
                toolName, toolDescription, JsonSchemaGenerator.fromMethod(method), beanClass, method);
    }

    public org.mcp_java.model.tool.Tool toWireFormat() {
        return new org.mcp_java.model.tool.Tool(name, null, description, inputSchema, null, null, null);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonObject getInputSchema() {
        return inputSchema;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public Method getMethod() {
        return method;
    }
}
