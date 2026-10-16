package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.cdi.telemetry.conventions.GenAIAttributes;
import dev.langchain4j.cdi.telemetry.conventions.GenAIOperations;
import dev.langchain4j.cdi.telemetry.conventions.GenAIProviders;
import dev.langchain4j.model.embedding.listener.EmbeddingModelErrorContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelRequestContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelResponseContext;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.request.EmbeddingRequestParameters;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.util.Map;

/**
 * Creates metrics that follow the <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/">Semantic
 * Conventions for GenAI spans</a>.
 *
 * @author Buhake Sindi
 * @since 12 October 2026
 */
@Dependent
public class GenAISpanEmbeddingModelListener extends GenAITracingTelemetry implements EmbeddingModelListener {

    /** Creates a new instance. */
    public GenAISpanEmbeddingModelListener() {}

    private static final String OTEL_SCOPE_KEY_NAME = "OTelScope_Embedding";
    private static final String OTEL_SPAN_KEY_NAME = "OTelSpan_Embedding";

    @Inject
    private Tracer tracer;

    @Override
    public void onRequest(EmbeddingModelRequestContext requestContext) {
        final EmbeddingRequest request = requestContext.embeddingRequest();
        SpanBuilder spanBuilder = tracer.spanBuilder(
                        GenAIOperations.EMBEDDINGS + " " + request.parameters().modelName())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GenAIAttributes.OPERATION_NAME.toString(), GenAIOperations.EMBEDDINGS.toString())
                .setAttribute(
                        GenAIAttributes.PROVIDER_NAME.toString(),
                        GenAIProviders.of(requestContext.modelProvider()).toString());

        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();

        final EmbeddingRequestParameters parameters = request.parameters();

        span.setAttribute(GenAIAttributes.REQUEST_MODEL.toString(), parameters.modelName());

        if (parameters.dimensions() != null)
            span.setAttribute(GenAIAttributes.EMBEDDINGS_DIMENSION_COUNT.toString(), parameters.dimensions());

        saveScope(scope, requestContext.attributes());
        saveSpan(span, requestContext.attributes());
    }

    @Override
    public void onResponse(EmbeddingModelResponseContext responseContext) {
        Span span = retrieveSpan(responseContext.attributes());
        if (span != null) {
            final EmbeddingResponse response = responseContext.embeddingResponse();
            span.setAttribute(GenAIAttributes.RESPONSE_MODEL.toString(), response.modelName());
            TokenUsage tokenUsage = response.metadata().tokenUsage();
            if (tokenUsage != null) {
                if (tokenUsage.outputTokenCount() != null)
                    span.setAttribute(GenAIAttributes.USAGE_OUTPUT_TOKENS.toString(), tokenUsage.outputTokenCount());

                if (tokenUsage.inputTokenCount() != null)
                    span.setAttribute(GenAIAttributes.USAGE_INPUT_TOKENS.toString(), tokenUsage.inputTokenCount());
            }

            // Finally...
            span.end();
        }

        closeScope(retrieveScope(responseContext.attributes()));
    }

    @Override
    public void onError(EmbeddingModelErrorContext errorContext) {
        Span span = retrieveSpan(errorContext.attributes());
        if (span != null) {
            traceException(span, errorContext.error());
            span.end();
        }

        closeScope(retrieveScope(errorContext.attributes()));
    }

    private static Span retrieveSpan(final Map<Object, Object> attributes) {
        return (Span) attributes.remove(OTEL_SPAN_KEY_NAME);
    }

    private static void saveSpan(final Span span, final Map<Object, Object> attributes) {
        attributes.put(OTEL_SPAN_KEY_NAME, span);
    }

    private static Scope retrieveScope(final Map<Object, Object> attributes) {
        return (Scope) attributes.remove(OTEL_SCOPE_KEY_NAME);
    }

    private static void saveScope(final Scope scope, final Map<Object, Object> attributes) {
        attributes.put(OTEL_SCOPE_KEY_NAME, scope);
    }
}
