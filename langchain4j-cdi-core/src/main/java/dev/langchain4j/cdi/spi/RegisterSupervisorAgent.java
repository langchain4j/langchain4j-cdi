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

    // TODO: add beforeCallName when SupervisorAgentService exposes beforeCall()

    String[] subAgentNames() default {};

    String chatModelName() default "#default";

    String chatMemoryProviderName() default "";

    int maxAgentsInvocations() default 10;

    String supervisorContext() default "";

    SupervisorContextStrategy supervisorContextStrategy() default SupervisorContextStrategy.CHAT_MEMORY;

    SupervisorResponseStrategy supervisorResponseStrategy() default SupervisorResponseStrategy.LAST;
}
