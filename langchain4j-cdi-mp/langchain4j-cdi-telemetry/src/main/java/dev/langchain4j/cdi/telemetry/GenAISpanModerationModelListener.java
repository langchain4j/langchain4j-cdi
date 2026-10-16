package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.cdi.telemetry.conventions.GenAIAttributes;
import dev.langchain4j.cdi.telemetry.conventions.GenAIProviders;
import dev.langchain4j.model.moderation.ModerationRequest;
import dev.langchain4j.model.moderation.ModerationResponse;
import dev.langchain4j.model.moderation.listener.ModerationModelErrorContext;
import dev.langchain4j.model.moderation.listener.ModerationModelListener;
import dev.langchain4j.model.moderation.listener.ModerationModelRequestContext;
import dev.langchain4j.model.moderation.listener.ModerationModelResponseContext;
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
public class GenAISpanModerationModelListener extends GenAITracingTelemetry implements ModerationModelListener {

    /** Creates a new instance. */
    public GenAISpanModerationModelListener() {}

    private static final String OTEL_SCOPE_KEY_NAME = "OTelScope_Moderation";
    private static final String OTEL_SPAN_KEY_NAME = "OTelSpan_Moderation";

    @Inject
    private Tracer tracer;

    @Override
    public void onRequest(ModerationModelRequestContext requestContext) {
        // TODO Auto-generated method stub
        final ModerationRequest request = requestContext.moderationRequest();
        SpanBuilder spanBuilder = tracer.spanBuilder("moderation " + request.modelName())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(GenAIAttributes.OPERATION_NAME.toString(), "moderation")
                .setAttribute(
                        GenAIAttributes.PROVIDER_NAME.toString(),
                        GenAIProviders.of(requestContext.modelProvider()).toString())
                .setAttribute(GenAIAttributes.REQUEST_MODEL.toString(), request.modelName());

        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();

        saveScope(scope, requestContext.attributes());
        saveSpan(span, requestContext.attributes());
    }

    @Override
    public void onResponse(ModerationModelResponseContext responseContext) {
        // TODO Auto-generated method stub
        Span span = retrieveSpan(responseContext.attributes());
        if (span != null) {
            final ModerationResponse response = responseContext.moderationResponse();
            span.setAttribute("moderation.flagged", response.moderation().flagged());

            // Finally...
            span.end();
        }

        closeScope(retrieveScope(responseContext.attributes()));
    }

    @Override
    public void onError(ModerationModelErrorContext errorContext) {
        // TODO Auto-generated method stub
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
