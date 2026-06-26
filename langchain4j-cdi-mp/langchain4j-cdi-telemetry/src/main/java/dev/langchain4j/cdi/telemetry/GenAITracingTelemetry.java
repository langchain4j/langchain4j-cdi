/** */
package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Buhake Sindi
 * @since 25 June 2026
 */
public abstract class GenAITracingTelemetry {

    private static final Map<ModelProvider, String> GEN_AI_PROVIDERS;

    static {
        Map<ModelProvider, String> providers = new LinkedHashMap<>();
        providers.put(ModelProvider.ANTHROPIC, "anthropic");
        providers.put(ModelProvider.AMAZON_BEDROCK, "aws.bedrock");
        providers.put(ModelProvider.AZURE_OPEN_AI, "azure.ai.openai");
        providers.put(ModelProvider.GOOGLE_AI_GEMINI, "gcp.gemini");
        providers.put(ModelProvider.GOOGLE_VERTEX_AI_ANTHROPIC, "gcp.vertex_ai");
        providers.put(ModelProvider.GOOGLE_VERTEX_AI_GEMINI, "gcp.vertex_ai");
        providers.put(ModelProvider.MISTRAL_AI, "mistral_ai");
        providers.put(ModelProvider.OPEN_AI, "openai");
        providers.put(ModelProvider.WATSONX, "ibm.watsonx.ai");

        Arrays.stream(ModelProvider.values())
                .filter(provider -> !providers.containsKey(provider))
                .forEach(provider -> providers.put(provider, provider.toString().toLowerCase()));

        GEN_AI_PROVIDERS = Collections.unmodifiableMap(providers);
    }

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    protected void traceModelProvider(final Span span, final ModelProvider provider) {
        span.setAttribute("gen_ai.provider.name", GEN_AI_PROVIDERS.get(provider));
    }

    protected void traceChatRequest(final Span span, final ChatRequest request) {
        final List<ChatMessage> inputMessages = new ArrayList<>();
        final List<ChatMessage> systemInstructions = new ArrayList<>();

        final ChatRequestParameters parameters = request.parameters();

        if (parameters.maxOutputTokens() != null)
            span.setAttribute("gen_ai.request.max_tokens", parameters.maxOutputTokens());

        if (parameters.temperature() != null) span.setAttribute("gen_ai.request.temperature", parameters.temperature());

        if (parameters.topK() != null) span.setAttribute("gen_ai.request.top_k", parameters.topK());

        if (parameters.topP() != null) span.setAttribute("gen_ai.request.top_p", parameters.topP());

        if (parameters.presencePenalty() != null)
            span.setAttribute("gen_ai.request.presence_penalty", parameters.presencePenalty());

        if (parameters.frequencyPenalty() != null)
            span.setAttribute("gen_ai.request.frequency_penalty", parameters.frequencyPenalty());

        if (parameters.stopSequences() != null)
            span.setAttribute(AttributeKey.stringArrayKey("gen_ai.request.stop_sequences"), parameters.stopSequences());

        captureFurtherRequests(span, request);

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
                    "gen_ai.tool.definitions",
                    "["
                            + parameters.toolSpecifications().stream()
                                    .map(ToolSpecification::toJson)
                                    .collect(Collectors.joining(", "))
                            + "]");
        }
    }

    private void captureFurtherRequests(final Span span, final ChatRequest request) {
        final ChatRequestParameters parameters = request.parameters();

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

    protected void traceChatResponse(final Span span, final ChatResponse response) {
        span.setAttribute("gen_ai.response.id", response.metadata().id())
                .setAttribute("gen_ai.response.model", response.metadata().modelName());
        if (response.finishReason() != null) {
            span.setAttribute(
                    AttributeKey.stringArrayKey("gen_ai.response.finish_reasons"),
                    Arrays.asList(response.metadata().finishReason().toString()));
        }
        TokenUsage tokenUsage = response.metadata().tokenUsage();
        if (tokenUsage != null) {
            span.setAttribute("gen_ai.usage.output_tokens", tokenUsage.outputTokenCount())
                    .setAttribute("gen_ai.usage.input_tokens", tokenUsage.inputTokenCount());
        }

        if (response.aiMessage() != null) {
            span.setAttribute(
                    "gen_ai.output.messages", ChatMessageSerializer.messagesToJson(List.of(response.aiMessage())));
        }
    }

    protected void traceException(final Span span, final Throwable exception) {
        span.recordException(exception);
    }
}
