package dev.langchain4j.cdi.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Stereotype to register an interface as a LangChain4j SUPERVISOR agent that orchestrates sub-agents via an LLM. */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterSupervisorAgent {

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

    /**
     * CDI bean name of an error handler invoked when a sub-agent fails.
     *
     * @return the error handler bean name
     */
    String errorHandlerName() default "";

    /**
     * CDI bean name of a function that provides the agent output from the agentic scope.
     *
     * @return the output provider bean name
     */
    String outputProviderName() default "";

    // TODO: add beforeCallName when SupervisorAgentService exposes beforeCall()

    /**
     * Names of the sub-agents available for the supervisor to orchestrate.
     *
     * @return the sub-agent names
     */
    String[] subAgentNames() default {};

    /**
     * CDI bean name of the chat model used by the supervisor. Use {@code "#default"} for the default bean.
     *
     * @return the chat model bean name
     */
    String chatModelName() default "#default";

    /**
     * CDI bean name of the chat memory provider. Blank means no provider is wired.
     *
     * @return the chat memory provider bean name
     */
    String chatMemoryProviderName() default "";

    /**
     * Maximum number of sub-agent invocations the supervisor may perform.
     *
     * @return the maximum invocation count
     */
    int maxAgentsInvocations() default 10;

    /**
     * Additional context information for the supervisor prompt.
     *
     * @return the supervisor context
     */
    String supervisorContext() default "";

    /**
     * Strategy for how sub-agent context is shared with the supervisor.
     *
     * @return the context strategy
     */
    SupervisorContextStrategy supervisorContextStrategy() default SupervisorContextStrategy.CHAT_MEMORY;

    /**
     * Strategy for selecting the final response from sub-agent results.
     *
     * @return the response strategy
     */
    SupervisorResponseStrategy supervisorResponseStrategy() default SupervisorResponseStrategy.LAST;
}
