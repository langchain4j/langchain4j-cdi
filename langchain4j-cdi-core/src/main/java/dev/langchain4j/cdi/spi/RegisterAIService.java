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

    /**
     * CDI scope for the AI service bean.
     *
     * @return the scope annotation class
     */
    Class<? extends Annotation> scope() default RequestScoped.class;

    /**
     * Tool classes to wire into the AI service. Each class is resolved as a CDI bean when possible, or instantiated via
     * its no-arg constructor otherwise. Can be used together with {@link #toolProviderName()}: both are applied when
     * present. Avoid overlapping tool names across the two sources — LangChain4j will throw
     * {@code IllegalConfigurationException} at runtime if the same tool name appears more than once.
     *
     * @return the tool classes
     */
    Class<?>[] tools() default {};

    /**
     * CDI bean name of the chat model. Use "#default" for the default bean, or blank to skip.
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
     * CDI bean name of the content retriever. Blank means no retriever is wired.
     *
     * @return the content retriever bean name
     */
    String contentRetrieverName() default "";

    /**
     * CDI bean name of the moderation model. Blank means no moderation is applied.
     *
     * @return the moderation model bean name
     */
    String moderationModelName() default "";

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
     * CDI bean name of the retrieval augmentor. Takes precedence over content retriever when set.
     *
     * @return the retrieval augmentor bean name
     */
    String retrievalAugmentorName() default "";

    /**
     * Name of a CDI {@link dev.langchain4j.service.tool.ToolProvider} bean to wire into the AI service. Can be used
     * together with {@link #tools()}: both are applied when present. Avoid overlapping tool names across the two
     * sources — LangChain4j will throw {@code IllegalConfigurationException} at runtime if the same tool name appears
     * more than once. Blank means no {@code ToolProvider} is wired.
     *
     * @return the tool provider bean name
     */
    String toolProviderName() default "";

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
