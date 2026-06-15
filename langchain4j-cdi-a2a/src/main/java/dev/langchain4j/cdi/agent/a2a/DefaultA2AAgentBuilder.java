package dev.langchain4j.cdi.agent.a2a;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.internal.A2AClientBuilder;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.cdi.agent.spi.A2AAgentBuilder;
import dev.langchain4j.cdi.aiservice.CdiLookupHelper;
import jakarta.enterprise.inject.Instance;

public class DefaultA2AAgentBuilder implements A2AAgentBuilder {

    @Override
    public <X> X build(
            Class<X> interfaceClass,
            String url,
            String outputKey,
            boolean async,
            boolean optional,
            String agentListenerName,
            Instance<Object> lookup) {
        // Defensive: createForA2A already validates the URL before calling this builder,
        // but guard here so the SPI contract is self-contained for any external implementors.
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "@RegisterA2AAgent on " + interfaceClass.getSimpleName() + ": 'a2aServerUrl' is required.");
        }
        A2AClientBuilder<X> builder = AgenticServices.a2aBuilder(url, interfaceClass);
        if (outputKey != null && !outputKey.isBlank()) {
            builder.outputKey(outputKey);
        }
        builder.async(async);
        AgentListener listener = CdiLookupHelper.resolveSingle(lookup, AgentListener.class, agentListenerName);
        if (listener != null) {
            builder.listener(listener);
        }
        return builder.build();
    }
}
