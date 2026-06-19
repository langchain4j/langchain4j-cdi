package dev.langchain4j.cdi.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stereotype to register an interface as a LangChain4j AI Service.
 *
 * <p>Apply it on an interface that will be implemented dynamically by LangChain4j AiServices. You can optionally
 * reference named CDI beans to wire the service: models, retrievers, tools, memories, etc. If a property name is blank,
 * the dependency is ignored. For chatModelName, "#default" means select the default bean.
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
@Stereotype
public @interface RegisterAIService {

    Class<? extends Annotation> scope() default RequestScoped.class;

    /**
     * Tool classes to wire into the AI service. Each class is resolved as a CDI bean when possible, or instantiated via
     * its no-arg constructor otherwise. Can be used together with {@link #toolProviderName()}: both are applied when
     * present. Avoid overlapping tool names across the two sources — LangChain4j will throw
     * {@code IllegalConfigurationException} at runtime if the same tool name appears more than once.
     */
    Class<?>[] tools() default {};

    String chatModelName() default "#default";

    String streamingChatModelName() default "";

    String contentRetrieverName() default "";

    String moderationModelName() default "";

    String chatMemoryName() default "";

    String chatMemoryProviderName() default "";

    String retrievalAugmentorName() default "";

    /**
     * Name of a CDI {@link dev.langchain4j.service.tool.ToolProvider} bean to wire into the AI service. Can be used
     * together with {@link #tools()}: both are applied when present. Avoid overlapping tool names across the two
     * sources — LangChain4j will throw {@code IllegalConfigurationException} at runtime if the same tool name appears
     * more than once. Blank means no {@code ToolProvider} is wired.
     */
    String toolProviderName() default "";

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
