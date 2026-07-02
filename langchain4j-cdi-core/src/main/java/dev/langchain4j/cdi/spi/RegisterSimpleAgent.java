package dev.langchain4j.cdi.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stereotype to register an interface as a LangChain4j SIMPLE agent backed by a plain AI service.
 *
 * <p>All CDI dependencies are resolved exclusively by name. Use {@code "#default"} to select the default bean of a
 * type, leave blank to skip, or provide an explicit bean name (or an expression resolved via
 * {@code ExpressionResolver}).
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterSimpleAgent {

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

    // TODO: add errorHandlerName when AgentBuilder exposes errorHandler()

    /**
     * CDI bean name of the chat model. Use {@code "#default"} for the default bean, or blank to skip.
     *
     * @return the chat model bean name
     */
    String chatModelName() default "#default";

    /**
     * CDI bean name of the streaming chat model. Blank means no streaming model is wired.
     *
     * @return the streaming chat model bean name
     */
    String streamingChatModelName() default "";

    /**
     * Tool classes to wire into the agent. Each class is resolved as a CDI bean when possible, or instantiated via its
     * no-arg constructor otherwise. Can be used together with {@link #toolNames()} and {@link #toolProviderName()}: all
     * present sources are applied. Avoid overlapping tool names across sources — LangChain4j will throw
     * {@code IllegalConfigurationException} at runtime if the same tool name appears more than once.
     *
     * @return the tool classes
     */
    Class<?>[] tools() default {};

    /**
     * CDI bean names of tool objects to register. Each name must resolve to a {@code @Named} CDI bean. Can be used
     * together with {@link #tools()} and {@link #toolProviderName()}.
     *
     * @return the tool bean names
     */
    String[] toolNames() default {};

    /**
     * CDI bean name of the tool provider. Blank means no provider is wired.
     *
     * @return the tool provider bean name
     */
    String toolProviderName() default "";

    /**
     * CDI bean name of the chat memory. Blank means no chat memory is wired.
     *
     * @return the chat memory bean name
     */
    String chatMemoryName() default "";

    /**
     * CDI bean name of the chat memory provider. Blank means no provider is wired.
     *
     * @return the chat memory provider bean name
     */
    String chatMemoryProviderName() default "";

    /**
     * CDI bean name of the content retriever. Blank means no retriever is wired.
     *
     * @return the content retriever bean name
     */
    String contentRetrieverName() default "";

    /**
     * CDI bean name of the retrieval augmentor. Takes precedence over content retriever when set.
     *
     * @return the retrieval augmentor bean name
     */
    String retrievalAugmentorName() default "";

    /**
     * Input guardrail classes to validate messages before sending to the LLM. If a class is a CDI managed bean, the
     * bean instance is used; otherwise it is instantiated via its no-arg constructor. Mutually exclusive with
     * {@link #inputGuardrailNames()}: if both are specified, only the classes are used and the names are ignored.
     *
     * @return the input guardrail classes
     */
    Class<? extends InputGuardrail>[] inputGuardrails() default {};

    /**
     * Output guardrail classes to validate LLM responses before returning them. If a class is a CDI managed bean, the
     * bean instance is used; otherwise it is instantiated via its no-arg constructor. Mutually exclusive with
     * {@link #outputGuardrailNames()}: if both are specified, only the classes are used and the names are ignored.
     *
     * @return the output guardrail classes
     */
    Class<? extends OutputGuardrail>[] outputGuardrails() default {};

    /**
     * Named CDI beans implementing {@link InputGuardrail} to validate messages before sending to the LLM. Unresolvable
     * names are skipped with a WARNING log. Mutually exclusive with {@link #inputGuardrails()}: if both are specified,
     * only the classes are used and the names are ignored.
     *
     * @return the input guardrail bean names
     */
    String[] inputGuardrailNames() default {};

    /**
     * Named CDI beans implementing {@link OutputGuardrail} to validate LLM responses before returning them.
     * Unresolvable names are skipped with a WARNING log. Mutually exclusive with {@link #outputGuardrails()}: if both
     * are specified, only the classes are used and the names are ignored.
     *
     * @return the output guardrail bean names
     */
    String[] outputGuardrailNames() default {};
}
