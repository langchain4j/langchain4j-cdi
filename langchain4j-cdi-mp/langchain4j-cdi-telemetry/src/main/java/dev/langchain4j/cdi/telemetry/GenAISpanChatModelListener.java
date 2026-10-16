package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.cdi.telemetry.conventions.GenAIAttributes;
import dev.langchain4j.cdi.telemetry.conventions.GenAIOperations;
import dev.langchain4j.cdi.telemetry.conventions.GenAIProviders;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
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
 * @since 25 November 2024
 */
@Dependent
public class GenAISpanChatModelListener extends GenAITracingTelemetry implements ChatModelListener {

    /** Creates a new instance. */
    public GenAISpanChatModelListener() {}

    private static final String OTEL_SCOPE_KEY_NAME = "OTelScope_Chat";
    private static final String OTEL_SPAN_KEY_NAME = "OTelSpan_Chat";

    @Inject
    private Tracer tracer;

    // ---------------------------------- Chat Model ----------------------------------

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        final ChatRequest request = requestContext.chatRequest();
        SpanBuilder spanBuilder = tracer.spanBuilder(
                        GenAIOperations.CHAT + " " + request.parameters().modelName())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GenAIAttributes.OPERATION_NAME.toString(), GenAIOperations.CHAT.toString())
                .setAttribute(
                        GenAIAttributes.PROVIDER_NAME.toString(),
                        GenAIProviders.of(requestContext.modelProvider()).toString());

        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();

        traceChatRequest(span, request);

        saveScope(scope, requestContext.attributes());
        saveSpan(span, requestContext.attributes());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Span span = retrieveSpan(responseContext.attributes());
        if (span != null) {
            traceChatResponse(span, responseContext.chatResponse());

            // Finally...
            span.end();
        }

        closeScope(retrieveScope(responseContext.attributes()));
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
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
