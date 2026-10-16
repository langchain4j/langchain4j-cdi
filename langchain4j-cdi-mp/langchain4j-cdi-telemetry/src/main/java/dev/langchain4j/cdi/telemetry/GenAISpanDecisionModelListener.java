package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.cdi.telemetry.conventions.GenAIAttributes;
import dev.langchain4j.cdi.telemetry.conventions.GenAIOperations;
import dev.langchain4j.cdi.telemetry.conventions.GenAIProviders;
import dev.langchain4j.model.decision.listener.DecisionModelErrorContext;
import dev.langchain4j.model.decision.listener.DecisionModelListener;
import dev.langchain4j.model.decision.listener.DecisionModelRequestContext;
import dev.langchain4j.model.decision.listener.DecisionModelResponseContext;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.request.DecisionRequestParameters;
import dev.langchain4j.model.decision.response.DecisionResponse;
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
 * Creates metrics that follow the <a href= "https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/">Semantic
 * Conventions for GenAI spans</a>.
 *
 * @author Buhake Sindi
 * @since 12 October 2026
 */
@Dependent
public class GenAISpanDecisionModelListener extends GenAITracingTelemetry implements DecisionModelListener {

    /** Creates a new instance. */
    public GenAISpanDecisionModelListener() {}

    private static final String OTEL_SCOPE_KEY_NAME = "OTelScope_Decision";
    private static final String OTEL_SPAN_KEY_NAME = "OTelSpan_Decision";

    @Inject
    private Tracer tracer;

    @Override
    public void onRequest(DecisionModelRequestContext requestContext) {
        final DecisionRequest request = requestContext.decisionRequest();
        SpanBuilder spanBuilder = tracer.spanBuilder(GenAIOperations.DECISION.toString() + " "
                        + request.parameters().modelName())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GenAIAttributes.OPERATION_NAME.toString(), GenAIOperations.DECISION.toString())
                .setAttribute(
                        GenAIAttributes.PROVIDER_NAME.toString(),
                        GenAIProviders.of(requestContext.modelProvider()).toString());

        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();

        final DecisionRequestParameters parameters = request.parameters();

        span.setAttribute(GenAIAttributes.REQUEST_MODEL.toString(), parameters.modelName());

        saveScope(scope, requestContext.attributes());
        saveSpan(span, requestContext.attributes());
    }

    @Override
    public void onResponse(DecisionModelResponseContext responseContext) {
        Span span = retrieveSpan(responseContext.attributes());
        if (span != null) {
            final DecisionResponse response = responseContext.decisionResponse();
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
    public void onError(DecisionModelErrorContext errorContext) {
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
