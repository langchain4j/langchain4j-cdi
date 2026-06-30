/** */
package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Creates metrics that follow the <a
 * href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-agent-spans/">Semantic Conventions for GenAI agent
 * and framework spans</a>.
 *
 * @author Buhake Sindi
 * @since 20 June 2026
 */
@Dependent
public class SpanAgentListener extends GenAITracingTelemetry implements AgentListener {

    /** Creates a new instance. */
    public SpanAgentListener() {}

    private static final GenAIOperations OPERATION_INVOKE_AGENT = GenAIOperations.INVOKE_AGENT;

    @Inject
    private Tracer tracer;

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        Span span = createInvokeAgentSpan(agentResponse.agenticScope(), agentResponse.agent());
        try (Scope scope = span.makeCurrent()) {
            if (agentResponse.chatRequest() != null) traceChatRequest(span, agentResponse.chatRequest());
            if (agentResponse.chatResponse() != null) traceChatResponse(span, agentResponse.chatResponse());
        } finally {
            span.end();
        }
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError agentInvocationError) {
        Span span = createInvokeAgentSpan(agentInvocationError.agenticScope(), agentInvocationError.agent());
        try (Scope scope = span.makeCurrent()) {
            traceException(span, agentInvocationError.error());
        } finally {
            span.end();
        }
    }

    private Span createInvokeAgentSpan(final AgenticScope agenticScope, final AgentInstance agent) {
        SpanBuilder spanBuilder = tracer.spanBuilder(GenAIOperations.INVOKE_AGENT.toString() + " " + agent.name())
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("gen_ai.operation.name", OPERATION_INVOKE_AGENT.toString())
                .setAttribute("gen_ai.agent.description", agent.description())
                .setAttribute("gen_ai.agent.id", agent.agentId())
                .setAttribute("gen_ai.agent.name", agent.name());

        if (agenticScope.memoryId() != null) {
            spanBuilder.setAttribute("gen_ai.conversation.id", String.valueOf(agenticScope.memoryId()));
        }

        return spanBuilder.startSpan();
    }
}
