package dev.langchain4j.cdi.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

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

    Class<? extends Annotation> scope() default ApplicationScoped.class;

    String name() default "";

    String description() default "";

    String outputKey() default "";

    boolean async() default false;

    String agentListenerName() default "";

    String chatModelName() default "#default";

    String streamingChatModelName() default "";

    /**
     * Tool classes to wire into the agent. Each class is resolved as a CDI bean when possible, or instantiated via its
     * no-arg constructor otherwise. Can be used together with {@link #toolNames()} and {@link #toolProviderName()}: all
     * present sources are applied. Avoid overlapping tool names across sources — LangChain4j will throw
     * {@code IllegalConfigurationException} at runtime if the same tool name appears more than once.
     */
    Class<?>[] tools() default {};

    /**
     * CDI bean names of tool objects to register. Each name must resolve to a {@code @Named} CDI bean. Can be used
     * together with {@link #tools()} and {@link #toolProviderName()}.
     */
    String[] toolNames() default {};

    String toolProviderName() default "";

    String chatMemoryName() default "";

    String chatMemoryProviderName() default "";

    String contentRetrieverName() default "";

    String retrievalAugmentorName() default "";

    /**
     * Input guardrail classes to validate messages before sending to the LLM. If a class is a CDI managed bean, the
     * bean instance is used; otherwise it is instantiated via its no-arg constructor. Mutually exclusive with
     * {@link #inputGuardrailNames()}: if both are specified, only the classes are used and the names are ignored.
     */
    Class<? extends InputGuardrail>[] inputGuardrails() default {};

    /**
     * Output guardrail classes to validate LLM responses before returning them. If a class is a CDI managed bean, the
     * bean instance is used; otherwise it is instantiated via its no-arg constructor. Mutually exclusive with
     * {@link #outputGuardrailNames()}: if both are specified, only the classes are used and the names are ignored.
     */
    Class<? extends OutputGuardrail>[] outputGuardrails() default {};

    /**
     * Named CDI beans implementing {@link InputGuardrail} to validate messages before sending to the LLM. Unresolvable
     * names are skipped with a WARNING log. Mutually exclusive with {@link #inputGuardrails()}: if both are specified,
     * only the classes are used and the names are ignored.
     */
    String[] inputGuardrailNames() default {};

    /**
     * Named CDI beans implementing {@link OutputGuardrail} to validate LLM responses before returning them.
     * Unresolvable names are skipped with a WARNING log. Mutually exclusive with {@link #outputGuardrails()}: if both
     * are specified, only the classes are used and the names are ignored.
     */
    String[] outputGuardrailNames() default {};
}
