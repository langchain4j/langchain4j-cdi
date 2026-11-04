package dev.langchain4j.cdi.mcp.server.registry;

import dev.langchain4j.cdi.mcp.server.api.McpApiFactory;
import dev.langchain4j.cdi.mcp.server.api.McpFrameworkTypes;
import dev.langchain4j.cdi.mcp.server.api.McpRequestContext;
import dev.langchain4j.cdi.mcp.server.error.McpErrorCode;
import dev.langchain4j.cdi.mcp.server.error.McpException;
import dev.langchain4j.cdi.mcp.server.schema.McpParameterNames;
import dev.langchain4j.cdi.mcp.server.transport.McpInputRequiredBatchSignal;
import dev.langchain4j.cdi.mcp.server.transport.McpInputRequiredSignal;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mcpjava.server.tools.Tool;

/** Shared utility for invoking CDI bean methods with JSON arguments. */
@ApplicationScoped
public class McpBeanInvoker {

    private static final Logger LOGGER = Logger.getLogger(McpBeanInvoker.class.getName());

    /** CDI-required default constructor. */
    public McpBeanInvoker() {}

    @Inject
    BeanManager beanManager;

    @Inject
    McpApiFactory apiFactory;

    /**
     * Container-specific {@link McpMethodInvoker} providers, consulted before reflection. Empty (never {@code null}) on
     * runtimes that do not ship an implementing module, e.g. Jakarta EE 10 / CDI 4.0.1 servers such as WildFly and
     * OpenLiberty.
     */
    @Inject
    Instance<McpInvokerProvider> invokerProviders;

    private final ConcurrentHashMap<InvokerCacheKey, Optional<McpMethodInvoker>> invokerCache =
            new ConcurrentHashMap<>();

    /**
     * Cache key for a provider lookup. The lookup depends on the bean type as well as the method, and the two cannot be
     * collapsed: registries collect MCP methods with {@code beanClass.getMethods()}, which returns the
     * <em>declaring</em> {@link Method} for an inherited method, so two beans extending a common base that declares an
     * annotated method produce {@link Method} objects that are {@code equals()}. Keyed on the method alone, the second
     * bean would reuse the invoker the container built for the first — and since those invokers are built
     * {@code withInstanceLookup()}, the container would then resolve and invoke the wrong bean instance, silently.
     *
     * @param beanType the CDI bean class the lookup was made for
     * @param method the method the lookup was made for
     */
    private record InvokerCacheKey(Class<?> beanType, Method method) {}

    /**
     * Invokes a method without MCP framework context (backward compatible).
     *
     * @param requestId the JSON-RPC request ID for error reporting
     * @param beanType the CDI bean class to look up and invoke
     * @param method the method to invoke on the resolved bean
     * @param arguments the JSON object containing method arguments
     * @return the method invocation result
     */
    public Object invoke(Object requestId, Class<?> beanType, Method method, JsonObject arguments) {
        return invoke(requestId, beanType, method, arguments, null, null);
    }

    /**
     * Invokes a method with MCP framework context, enabling framework type injection.
     *
     * @param requestId the JSON-RPC request ID for error reporting
     * @param beanType the CDI bean class to look up and invoke
     * @param method the method to invoke on the resolved bean
     * @param arguments the JSON object containing method arguments
     * @param ctx the MCP request context, or {@code null} if unavailable
     * @param session the MCP session, or {@code null} if unavailable
     * @return the method invocation result
     */
    public Object invoke(
            Object requestId,
            Class<?> beanType,
            Method method,
            JsonObject arguments,
            McpRequestContext ctx,
            McpSession session) {
        Optional<McpMethodInvoker> providedInvoker = invokerCache.computeIfAbsent(
                new InvokerCacheKey(beanType, method), k -> lookupInvoker(k.beanType(), k.method()));
        if (providedInvoker.isPresent()) {
            return invokeViaProvider(requestId, beanType, method, arguments, ctx, session, providedInvoker.get());
        }
        return invokeViaReflection(requestId, beanType, method, arguments, ctx, session);
    }

    /**
     * Consults every registered {@link McpInvokerProvider}, in the order supplied by CDI, and returns the first invoker
     * offered for the given method. Never returns {@code null}.
     *
     * @param beanType the CDI bean class declaring the method
     * @param method the method to find an invoker for
     * @return the first matching invoker, or {@link Optional#empty()} when no provider is present or none matches
     */
    private Optional<McpMethodInvoker> lookupInvoker(Class<?> beanType, Method method) {
        if (invokerProviders == null || invokerProviders.isUnsatisfied()) {
            return Optional.empty();
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (McpInvokerProvider provider : invokerProviders) {
            Optional<McpMethodInvoker> found = provider.lookup(beanType, method.getName(), parameterTypes);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Object invokeViaProvider(
            Object requestId,
            Class<?> beanType,
            Method method,
            JsonObject arguments,
            McpRequestContext ctx,
            McpSession session,
            McpMethodInvoker methodInvoker) {
        Object[] args = resolveArguments(requestId, method, arguments, ctx, session, beanType);
        if (methodInvoker.resolvesInstance()) {
            return invokeAndMapExceptions(requestId, method, () -> methodInvoker.invoke(null, args));
        }
        Bean<?> bean = resolveBean(requestId, beanType);
        CreationalContext<?> creationalCtx = beanManager.createCreationalContext(bean);
        try {
            Object instance = beanManager.getReference(bean, beanType, creationalCtx);
            return invokeAndMapExceptions(requestId, method, () -> methodInvoker.invoke(instance, args));
        } finally {
            creationalCtx.release();
        }
    }

    private Object invokeViaReflection(
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
            Object[] args = resolveArguments(requestId, method, arguments, ctx, session, beanType);
            return method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            throw mapInvocationException(requestId, method, e.getCause());
        } catch (IllegalArgumentException e) {
            // Method.invoke's "argument type mismatch": a parameter type the binder does not convert (an array, a
            // collection, a POJO). Answered -32603 like any other invocation failure, never propagated to the
            // container.
            throw mapInvocationException(requestId, method, e);
        } catch (IllegalAccessException e) {
            throw new McpException(requestId, McpErrorCode.INTERNAL_ERROR, "Invocation failed: " + method.getName(), e);
        } finally {
            creationalCtx.release();
        }
    }

    /**
     * Invokes the given call and maps its failures the same way as the reflective path: {@link McpInputRequiredSignal},
     * {@link McpInputRequiredBatchSignal} and {@link McpException} are rethrown unwrapped,
     * {@link InvocationTargetException} is unwrapped before mapping, and any other exception — including the checked
     * exceptions {@code jakarta.enterprise.invoke.Invoker#invoke} is declared to throw — is wrapped as
     * {@link McpErrorCode#INTERNAL_ERROR}.
     *
     * @param requestId the JSON-RPC request ID for error reporting
     * @param method the method being invoked, for error messages
     * @param call the invocation to perform
     * @return the call's result
     */
    private Object invokeAndMapExceptions(Object requestId, Method method, InvokerCall call) {
        try {
            return call.invoke();
        } catch (InvocationTargetException e) {
            throw mapInvocationException(requestId, method, e.getCause());
        } catch (McpInputRequiredSignal | McpInputRequiredBatchSignal | McpException e) {
            throw e;
        } catch (Exception e) {
            throw mapInvocationException(requestId, method, e);
        }
    }

    private RuntimeException mapInvocationException(Object requestId, Method method, Throwable cause) {
        if (cause instanceof McpInputRequiredSignal signal) {
            return signal;
        }
        if (cause instanceof McpInputRequiredBatchSignal batchSignal) {
            return batchSignal;
        }
        if (cause instanceof McpException mcpException) {
            return mcpException;
        }
        LOGGER.log(Level.WARNING, "MCP: invocation of " + method.getName() + " failed", cause);
        return new McpException(requestId, McpErrorCode.INTERNAL_ERROR, "Tool execution failed", cause);
    }

    /** A single invocation to perform, abstracting over reflective and provider-supplied invokers. */
    @FunctionalInterface
    private interface InvokerCall {
        Object invoke() throws Exception;
    }

    private Bean<?> resolveBean(Object requestId, Class<?> beanType) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(beanType));
        if (bean == null) {
            throw new McpException(
                    requestId, McpErrorCode.INTERNAL_ERROR, "CDI bean not found for: " + beanType.getName());
        }
        return bean;
    }

    /**
     * Builds the Java arguments of an invocation: MCP framework types are injected, every other parameter is bound from
     * its JSON argument by {@link McpArgumentBinder}. A {@code @Tool} method's arguments are typed JSON values
     * described by its {@code inputSchema}; the arguments of any other method (a prompt, a resource template) are
     * strings on the wire. An argument that cannot be bound raises {@code McpInvalidArgumentException}, an
     * {@link McpException}, which both invocation paths propagate unchanged.
     */
    private Object[] resolveArguments(
            Object requestId,
            Method method,
            JsonObject arguments,
            McpRequestContext ctx,
            McpSession session,
            Class<?> beanType) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        boolean textual = !method.isAnnotationPresent(Tool.class);

        for (int i = 0; i < params.length; i++) {
            if (McpFrameworkTypes.isFrameworkType(params[i].getType())) {
                args[i] = apiFactory.createInstance(params[i].getType(), ctx, session, beanType);
            } else {
                String paramName = McpParameterNames.resolve(params[i]);
                JsonValue value = arguments != null ? arguments.get(paramName) : null;
                args[i] = McpArgumentBinder.bind(requestId, params[i], paramName, value, textual);
            }
        }
        return args;
    }
}
