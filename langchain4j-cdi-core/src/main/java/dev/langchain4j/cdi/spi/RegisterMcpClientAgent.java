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

    /**
     * CDI scope for the agent bean.
     *
     * @return the scope annotation class
     */
    Class<? extends Annotation> scope() default ApplicationScoped.class;

    /**
     * Name of the agent.
     *
     * @return the agent name
     */
    String name() default "";

    /**
     * Description of the agent.
     *
     * @return the agent description
     */
    String description() default "";

    /**
     * Key for storing the agent output in the agentic scope.
     *
     * @return the output key
     */
    String outputKey() default "";

    /**
     * Type-safe output key; takes precedence over {@link #outputKey()} when set.
     *
     * @return the typed output key class
     */
    Class<? extends TypedKey<?>> typedOutputKey() default Agent.NoTypedKey.class;

    /**
     * Whether the agent should execute asynchronously.
     *
     * @return true if asynchronous
     */
    boolean async() default false;

    /**
     * If true, the agent's execution will be silently skipped when any of its arguments is missing in the agentic
     * scope, instead of making the agentic system's execution fail.
     *
     * @return true if the agent is optional
     */
    boolean optional() default false;

    /**
     * Names of other agents whose conversation context should be summarized and injected into this agent's prompt.
     *
     * @return the agent names for context summarization
     */
    String[] summarizedContext() default {};

    /**
     * CDI bean name of an AgentListener to observe agent lifecycle events.
     *
     * @return the agent listener bean name
     */
    String agentListenerName() default "";

    // TODO: add errorHandlerName when McpService exposes errorHandler()

    /**
     * CDI bean name of the MCP client object. Required.
     *
     * @return the MCP client bean name
     */
    String mcpClientName();

    /**
     * Name of the MCP server tool to invoke. Required.
     *
     * @return the MCP tool name
     */
    String mcpToolName();

    /**
     * Keys of the input arguments read from {@link dev.langchain4j.agentic.scope.AgenticScope}.
     *
     * @return the input argument keys
     */
    String[] mcpInputKeys() default {};
}
