/** */
package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.cdi.telemetry.conventions.GenAIAttributes;
import dev.langchain4j.cdi.telemetry.conventions.GenAIExceptionAttributes;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Base class providing OpenTelemetry span attribute population for GenAI semantic conventions.
 *
 * @author Buhake Sindi
 * @since 25 June 2026
 */
public abstract class GenAITracingTelemetry {

    /** Creates a new instance. */
    protected GenAITracingTelemetry() {}

    private static final Logger LOGGER = Logger.getLogger(GenAITracingTelemetry.class.getName());

    /**
     * Populates the span with GenAI request attributes from the given chat request.
     *
     * @param span the span to populate
     * @param request the chat request containing model parameters and messages
     */
    protected static void traceChatRequest(final Span span, final ChatRequest request) {
        traceChatRequest(span, request, false);
    }

    /**
     * Populates the span with GenAI request attributes from the given chat request.
     *
     * @param span the span to populate
     * @param request the chat request containing model parameters and messages
     * @param captureMessagesAndTools whether to trace all chat request system instructions, messages and tools
     */
    protected static void traceChatRequest(
            final Span span, final ChatRequest request, final boolean captureMessagesAndTools) {
        final ChatRequestParameters parameters = request.parameters();

        span.setAttribute(GenAIAttributes.REQUEST_MODEL.toString(), parameters.modelName());

        if (parameters.maxOutputTokens() != null)
            span.setAttribute(GenAIAttributes.REQUEST_MAX_TOKENS.toString(), parameters.maxOutputTokens());

        if (parameters.temperature() != null)
            span.setAttribute(GenAIAttributes.REQUEST_TEMPERATURE.toString(), parameters.temperature());

        if (parameters.topK() != null) span.setAttribute(GenAIAttributes.REQUEST_TOP_K.toString(), parameters.topK());

        if (parameters.topP() != null) span.setAttribute(GenAIAttributes.REQUEST_TOP_P.toString(), parameters.topP());

        if (parameters.presencePenalty() != null)
            span.setAttribute(GenAIAttributes.REQUEST_PRESENCE_PENALTY.toString(), parameters.presencePenalty());

        if (parameters.frequencyPenalty() != null)
            span.setAttribute(GenAIAttributes.REQUEST_FREQUENCY_PENALTY.toString(), parameters.frequencyPenalty());

        if (parameters.stopSequences() != null)
            span.setAttribute(
                    AttributeKey.stringArrayKey(GenAIAttributes.REQUEST_STOP_SEQUENCES.toString()),
                    parameters.stopSequences());

        traceFurtherAttributes(span, parameters);

        if (captureMessagesAndTools) traceChatRequestMessagesAndTools(span, request);
    }

    /**
     * Populates the span with GenAI request attributes from the given chat request.
     *
     * @param span the span to populate
     * @param request the chat request containing model parameters and messages
     */
    protected static void traceChatRequestMessagesAndTools(final Span span, final ChatRequest request) {
        final List<ChatMessage> inputMessages = new ArrayList<>();
        final List<ChatMessage> systemInstructions = new ArrayList<>();

        final ChatRequestParameters parameters = request.parameters();

        if (request.messages() != null) {
            request.messages().stream().forEach(message -> {
                if (message instanceof SystemMessage sm) systemInstructions.add(sm);
                else inputMessages.add(message);
            });
        }

        if (!inputMessages.isEmpty()) {
            span.setAttribute("gen_ai.input.messages", ChatMessageSerializer.messagesToJson(inputMessages));
        }

        if (!systemInstructions.isEmpty()) {
            span.setAttribute("gen_ai.system_instructions", ChatMessageSerializer.messagesToJson(systemInstructions));
        }

        if (parameters.toolSpecifications() != null) {
            span.setAttribute(
                    GenAIAttributes.TOOL_DEFINITIONS.toString(),
                    "["
                            + parameters.toolSpecifications().stream()
                                    .map(ToolSpecification::toJson)
                                    .collect(Collectors.joining(", "))
                            + "]");
        }
    }

    /**
     * Traces optional request attributes (seed, reasoning effort) via reflection.
     *
     * @param span the span to populate
     * @param parameters the chat request parameters
     */
    protected static void traceFurtherAttributes(final Span span, final ChatRequestParameters parameters) {
        try {
            final Method seedMethod = parameters.getClass().getMethod("seed");
            Object seed = seedMethod.invoke(parameters);
            if (seed instanceof Integer seedValue) span.setAttribute("gen_ai.request.seed", seedValue);
        } catch (NoSuchMethodException
                | SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.log(Level.FINE, "Failed to invoke optional method seed().", e);
        }

        try {
            final Method reasoningEffortMethod = parameters.getClass().getMethod("reasoningEffort");
            Object reasoningEffort = reasoningEffortMethod.invoke(parameters);
            if (reasoningEffort != null)
                span.setAttribute("gen_ai.request.reasoning.level", reasoningEffort.toString());
        } catch (NoSuchMethodException
                | SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.log(Level.FINE, "Failed to invoke optional method reasoningEffort().", e);
        }
    }

    /**
     * Populates the span with GenAI response attributes from the given chat response.
     *
     * @param span the span to populate
     * @param response the chat response containing metadata and token usage
     */
    protected static void traceChatResponse(final Span span, final ChatResponse response) {
        traceChatResponse(span, response, false);
    }

    /**
     * Populates the span with GenAI response attributes from the given chat response.
     *
     * @param span the span to populate
     * @param response the chat response containing metadata and token usage
     * @param captureMessages whether to trace all chat response messages
     */
    protected static void traceChatResponse(
            final Span span, final ChatResponse response, final boolean captureMessages) {
        span.setAttribute(
                        GenAIAttributes.RESPONSE_ID.toString(),
                        response.metadata().id())
                .setAttribute(GenAIAttributes.RESPONSE_MODEL.toString(), response.modelName());
        if (response.finishReason() != null) {
            span.setAttribute(
                    AttributeKey.stringArrayKey(GenAIAttributes.RESPONSE_FINISH_REASONS.toString()),
                    Arrays.asList(response.metadata().finishReason().toString()));
        }
        TokenUsage tokenUsage = response.metadata().tokenUsage();
        if (tokenUsage != null) {
            if (tokenUsage.outputTokenCount() != null)
                span.setAttribute(GenAIAttributes.USAGE_OUTPUT_TOKENS.toString(), tokenUsage.outputTokenCount());

            if (tokenUsage.inputTokenCount() != null)
                span.setAttribute(GenAIAttributes.USAGE_INPUT_TOKENS.toString(), tokenUsage.inputTokenCount());
        }

        if (captureMessages) traceChatResponseMessages(span, response);
    }

    /**
     * Populates the span with GenAI response attributes from the given chat response.
     *
     * @param span the span to populate
     * @param response the chat response containing metadata and token usage
     */
    protected static void traceChatResponseMessages(final Span span, final ChatResponse response) {
        if (response.aiMessage() != null) {
            span.setAttribute(
                    "gen_ai.output.messages", ChatMessageSerializer.messagesToJson(List.of(response.aiMessage())));
        }
    }

    /**
     * Records the given exception on the span. <br>
     * For more information, see <a
     * href="https://github.com/open-telemetry/semantic-conventions-genai/blob/main/docs/gen-ai/gen-ai-exceptions.md">Semantic
     * conventions for Generative AI exceptions</a>.
     *
     * @param span the span to record the exception on
     * @param exception the exception to record
     */
    protected static void traceException(final Span span, final Throwable exception) {
        StringWriter stacktraceWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stacktraceWriter));
        AttributesBuilder builder = Attributes.builder()
                .put(GenAIExceptionAttributes.EXCEPTION_MESSAGE.toString(), exception.getMessage())
                .put(
                        GenAIExceptionAttributes.EXCEPTION_TYPE.toString(),
                        exception.getClass().getName())
                .put(GenAIExceptionAttributes.EXCEPTION_STACKTRACE.toString(), stacktraceWriter.toString());
        span.addEvent(GenAIExceptionAttributes.CLIENT_OPERATION_EXCEPTION_EVENT.toString(), builder.build());
    }

    /**
     * Closes the given scope if non-null.
     *
     * @param scope the scope to close, may be {@code null}
     */
    protected static void closeScope(Scope scope) {
        if (scope != null) {
            scope.close();
        }
    }
}
