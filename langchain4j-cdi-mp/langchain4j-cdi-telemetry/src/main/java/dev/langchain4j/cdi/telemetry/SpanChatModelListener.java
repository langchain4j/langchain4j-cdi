package dev.langchain4j.cdi.telemetry;

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

/**
 * Creates metrics that follow the <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/">Semantic
 * Conventions for GenAI spans</a>.
 *
 * @author Buhake Sindi
 * @since 25 November 2024
 */
@Dependent
public class SpanChatModelListener extends GenAITracingTelemetry implements ChatModelListener {

    /** Creates a new SpanChatModelListener. */
    public SpanChatModelListener() {}

    private static final String OTEL_SCOPE_KEY_NAME = "OTelScope";
    private static final String OTEL_SPAN_KEY_NAME = "OTelSpan";
    private static final GenAIOperations GEN_AI_OPERATION = GenAIOperations.CHAT;

    @Inject
    private Tracer tracer;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        final ChatRequest request = requestContext.chatRequest();
        SpanBuilder spanBuilder = tracer.spanBuilder(
                        GEN_AI_OPERATION.toString() + " " + request.parameters().modelName())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("gen_ai.operation.name", GEN_AI_OPERATION.toString());

        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();

        if (requestContext.modelProvider() != null) traceModelProvider(span, requestContext.modelProvider());
        traceChatRequest(span, request);

        requestContext.attributes().put(OTEL_SCOPE_KEY_NAME, scope);
        requestContext.attributes().put(OTEL_SPAN_KEY_NAME, span);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Span span = (Span) responseContext.attributes().get(OTEL_SPAN_KEY_NAME);
        if (span != null) {
            traceChatResponse(span, responseContext.chatResponse());
            span.end();
        }

        closeScope((Scope) responseContext.attributes().get(OTEL_SCOPE_KEY_NAME));
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Span span = (Span) errorContext.attributes().get(OTEL_SPAN_KEY_NAME);
        if (span != null) {
            traceException(span, errorContext.error());
            span.end();
        }

        closeScope((Scope) errorContext.attributes().get(OTEL_SCOPE_KEY_NAME));
    }

    private void closeScope(Scope scope) {
        if (scope != null) {
            scope.close();
        }
    }
}
