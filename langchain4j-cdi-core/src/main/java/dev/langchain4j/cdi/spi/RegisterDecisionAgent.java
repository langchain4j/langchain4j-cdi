package dev.langchain4j.cdi.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.service.decision.ThresholdProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stereotype to register an interface as a LangChain4j Decision agent backed by a plain AI service.
 *
 * <p>All CDI dependencies are resolved exclusively by name. Use {@code "#default"} to select the default bean of a
 * type, leave blank to skip, or provide an explicit bean name (or an expression resolved via
 * {@code ExpressionResolver}).
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterDecisionAgent {

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
     * CDI bean name of an {@link AgentListener} to observe agent lifecycle events.
     *
     * @return the agent listener bean name
     */
    String agentListenerName() default "";

    // TODO: add errorHandlerName when AgentBuilder exposes errorHandler()

    /**
     * CDI bean name of the decision model. Use {@code "#default"} for the default bean, or blank to skip.
     *
     * @return the decision model bean name
     */
    String decisionModelName() default "#default";

    /**
     * CDI bean name of the {@link ThresholdProvider}. Blank means no threshold provider of that name is wired.
     *
     * @return the threshold provider bean name
     */
    String thresholdProviderName() default "";

    /**
     * Sets a fixed threshold for the {@code boolean} answers of this agent: the answer is {@code true} when the
     * probability of "yes" is at least the threshold. Mutually exclusive with {@link #thresholdProviderName()}, which
     * injects a {@link ThresholdProvider} via CDI. Thresholds are specific to a decision model and to its version.
     *
     * @return threshold value as {@code double}, between 0 and 1.
     */
    double threshold() default Double.NaN;

    /**
     * CDI bean name of a {@code Predicate<AgenticScope>} that controls the condition under which this agent runs when
     * it is invoked as a subagent of an agentic system. Blank means no activation condition is applied.
     *
     * @return the activation condition bean name
     */
    String activationConditionName() default "";

    /**
     * Description of the activation condition for Sets the condition under which this agent runs when it is invoked as
     * a subagent of an agentic system, with a human-readable description used in logs, listeners and reports..
     *
     * @return the activation condition description
     */
    String activationConditionDescription() default "";
}
