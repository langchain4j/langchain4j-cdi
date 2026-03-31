package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.api.McpApiFactory;
import dev.langchain4j.cdi.mcp.server.api.McpFrameworkTypes;
import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.mcp_java.annotations.tools.ToolArg;

/** Shared utility for invoking CDI bean methods with JSON arguments. */
@ApplicationScoped
public class McpBeanInvoker {

    private static final String DEFAULT_NAME = "<<element name>>";

    @Inject
    BeanManager beanManager;

    @Inject
    McpApiFactory apiFactory;

    /** Invokes a method without MCP framework context (backward compatible). */
    public Object invoke(Object requestId, Class<?> beanType, Method method, JsonObject arguments) {
        return invoke(requestId, beanType, method, arguments, null, null);
    }

    /** Invokes a method with MCP framework context, enabling framework type injection. */
    public Object invoke(
            Object requestId,
            Class<?> beanType,
            Method method,
            JsonObject arguments,
            McpRequestContext ctx,
            McpSession session) {
        Bean<?> bean = resolveBean(requestId, beanType);
        CreationalContext<?> creationalCtx = beanManager.createCreationalContext(bean);
        try {
            Object instance = beanManager.getReference(bean, beanType, creationalCtx);
            Object[] args = resolveArguments(method, arguments, ctx, session, beanType);
            return method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            throw new McpException(
                    requestId,
                    McpErrorCode.INTERNAL_ERROR,
                    "Invocation failed: " + method.getName() + " - "
                            + e.getCause().getMessage());
        } catch (IllegalAccessException e) {
            throw new McpException(requestId, McpErrorCode.INTERNAL_ERROR, "Invocation failed: " + method.getName());
        } finally {
            creationalCtx.release();
        }
    }

    private Bean<?> resolveBean(Object requestId, Class<?> beanType) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(beanType));
        if (bean == null) {
            throw new McpException(
                    requestId, McpErrorCode.INTERNAL_ERROR, "CDI bean not found for: " + beanType.getName());
        }
        return bean;
    }

    private Object[] resolveArguments(
            Method method, JsonObject arguments, McpRequestContext ctx, McpSession session, Class<?> beanType) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            if (McpFrameworkTypes.isFrameworkType(params[i].getType())) {
                args[i] = apiFactory.createInstance(params[i].getType(), ctx, session, beanType);
            } else {
                String paramName = resolveParamName(params[i]);
                if (arguments != null && arguments.containsKey(paramName)) {
                    args[i] = convertJsonValue(arguments.get(paramName), params[i].getType());
                } else {
                    args[i] = getDefaultValue(params[i].getType());
                }
            }
        }
        return args;
    }

    private static String resolveParamName(Parameter param) {
        ToolArg annotation = param.getAnnotation(ToolArg.class);
        if (annotation != null && !DEFAULT_NAME.equals(annotation.name())) {
            return annotation.name();
        }
        return param.getName();
    }

    private Object convertJsonValue(JsonValue jsonValue, Class<?> targetType) {
        if (jsonValue == null || jsonValue.getValueType() == JsonValue.ValueType.NULL) {
            return getDefaultValue(targetType);
        }
        if (targetType == String.class) {
            if (jsonValue instanceof JsonString) {
                return ((JsonString) jsonValue).getString();
            }
            return jsonValue.toString();
        }
        if (targetType == int.class || targetType == Integer.class) {
            return ((JsonNumber) jsonValue).intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return ((JsonNumber) jsonValue).longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return ((JsonNumber) jsonValue).doubleValue();
        }
        if (targetType == float.class || targetType == Float.class) {
            return (float) ((JsonNumber) jsonValue).doubleValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return jsonValue.getValueType() == JsonValue.ValueType.TRUE;
        }
        return jsonValue.toString();
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        return null;
    }
}
