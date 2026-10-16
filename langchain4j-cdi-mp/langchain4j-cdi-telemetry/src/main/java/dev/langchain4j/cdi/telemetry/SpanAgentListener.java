/** */
package dev.langchain4j.cdi.telemetry;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.cdi.telemetry.conventions.GenAIAttributes;
import dev.langchain4j.cdi.telemetry.conventions.GenAIOperations;
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
                .setAttribute(GenAIAttributes.OPERATION_NAME.toString(), GenAIOperations.INVOKE_AGENT.toString())
                .setAttribute(GenAIAttributes.AGENT_DESCRIPTION.toString(), agent.description())
                .setAttribute(GenAIAttributes.AGENT_ID.toString(), agent.agentId())
                .setAttribute(GenAIAttributes.AGENT_NAME.toString(), agent.name())
                .setAttribute("agent.topology", agent.topology().name());

        if (agenticScope.memoryId() != null) {
            spanBuilder.setAttribute(
                    GenAIAttributes.CONVERSATION_ID.toString(), String.valueOf(agenticScope.memoryId()));
        }

        return spanBuilder.startSpan();
    }
}
