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
 * Stereotype to register an interface as a LangChain4j PARALLEL_MAPPER agent that applies a single sub-agent to each
 * item in a list read from the {@link dev.langchain4j.agentic.scope.AgenticScope}.
 *
 * <p>{@link #itemsKey()} is required and must name the scope key holding the list of items to map over. The first entry
 * in {@link #subAgentNames()} is the worker agent applied to each item.
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterParallelMapperAgent {

    Class<? extends Annotation> scope() default ApplicationScoped.class;

    String name() default "";

    String description() default "";

    String outputKey() default "";

    Class<? extends TypedKey<?>> typedOutputKey() default Agent.NoTypedKey.class;

    /**
     * If true, the agent's execution will be silently skipped when any of its arguments is missing in the agentic
     * scope, instead of making the agentic system's execution fail.
     */
    boolean optional() default false;

    /** Names of other agents whose conversation context should be summarized and injected into this agent's prompt. */
    String[] summarizedContext() default {};

    String agentListenerName() default "";

    String errorHandlerName() default "";

    String outputProviderName() default "";

    String beforeCallName() default "";

    String[] subAgentNames() default {};

    /** Key in {@link dev.langchain4j.agentic.scope.AgenticScope} that holds the list of items to map over. Required. */
    String itemsKey();
}
