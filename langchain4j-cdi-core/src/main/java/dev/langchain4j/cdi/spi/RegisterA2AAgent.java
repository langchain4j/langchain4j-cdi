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
 * Stereotype to register an interface as a LangChain4j A2A (Agent-to-Agent) agent that delegates to a remote A2A
 * server.
 *
 * <p>{@link #a2aServerUrl()} is required. Add the {@code langchain4j-cdi-a2a} module to the classpath to activate the
 * A2A protocol implementation.
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterA2AAgent {

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

    // TODO: add errorHandlerName when A2AAgentBuilder exposes errorHandler()

    /** URL of the A2A server. Required. */
    String a2aServerUrl();
}
