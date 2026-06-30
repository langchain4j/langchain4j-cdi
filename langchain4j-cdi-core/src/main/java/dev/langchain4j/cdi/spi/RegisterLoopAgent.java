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

/** Stereotype to register an interface as a LangChain4j LOOP agent that iterates sub-agents repeatedly. */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterLoopAgent {

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

    /**
     * CDI bean name of a consumer invoked before the agent executes.
     *
     * @return the before-call hook bean name
     */
    String beforeCallName() default "";

    /**
     * Names of the sub-agents to execute in each loop iteration.
     *
     * @return the sub-agent names
     */
    String[] subAgentNames() default {};

    /**
     * Maximum number of loop iterations.
     *
     * @return the maximum iteration count
     */
    int maxIterations() default 10;

    /**
     * CDI bean name of a {@code Predicate<AgenticScope>} or {@code BiPredicate<AgenticScope, Integer>} that controls
     * early exit from the loop. Blank means no early exit condition is applied.
     *
     * @return the exit condition bean name
     */
    String exitConditionName() default "";

    /**
     * Description of the exit condition for LLM prompting.
     *
     * @return the exit condition description
     */
    String exitConditionDescription() default "";

    /**
     * Whether the exit condition should be tested after each iteration rather than only at the end.
     *
     * @return true to test the exit condition after each iteration
     */
    boolean testAfterEachIteration() default false;
}
