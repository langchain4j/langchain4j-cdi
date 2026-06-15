package dev.langchain4j.cdi.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.TypedKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stereotype to register an interface as a LangChain4j MCP_CLIENT agent that wraps an MCP server tool.
 *
 * <p>Both {@link #mcpClientName()} and {@link #mcpToolName()} are required.
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterMcpClientAgent {

    Class<? extends Annotation> scope() default ApplicationScoped.class;

    String name() default "";

    String description() default "";

    String outputKey() default "";

    Class<? extends TypedKey<?>> typedOutputKey() default Agent.NoTypedKey.class;

    boolean async() default false;

    /**
     * If true, the agent's execution will be silently skipped when any of its arguments is missing in the agentic
     * scope, instead of making the agentic system's execution fail.
     */
    boolean optional() default false;

    /** Names of other agents whose conversation context should be summarized and injected into this agent's prompt. */
    String[] summarizedContext() default {};

    String agentListenerName() default "";

    // TODO: add errorHandlerName when McpService exposes errorHandler()

    /** CDI bean name of the MCP client object. Required. */
    String mcpClientName();

    /** Name of the MCP server tool to invoke. Required. */
    String mcpToolName();

    /** Keys of the input arguments read from {@link dev.langchain4j.agentic.scope.AgenticScope}. */
    String[] mcpInputKeys() default {};
}
