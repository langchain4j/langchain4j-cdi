package dev.langchain4j.cdi.aiservice;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a {@code static} method on an AI service interface as the handler for the model's thinking/reasoning output.
 * The method is invoked after any non-streaming response of that service whose {@code AiMessage.thinking()} is
 * non-blank.
 *
 * <p>The handler must be {@code static}, return {@code void}, and take a single {@link ThinkingEmitted} parameter. Only
 * one such handler is allowed per AI service interface.
 *
 * <pre>{@code
 * @RegisterAIService
 * interface MathAssistant {
 *     String solve(String problem);
 *
 *     @OnThinking
 *     static void onThinking(ThinkingEmitted event) {
 *         System.out.printf("[%s] %s%n", event.methodName(), event.text());
 *     }
 * }
 * }</pre>
 *
 * <p>This annotation does <strong>not</strong> enable thinking on the model itself — each provider exposes its own
 * configuration for that.
 *
 * <p>For the streaming path, use the {@code onPartialThinking} handler exposed by {@code TokenStream}.
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
@Documented
public @interface OnThinking {}
