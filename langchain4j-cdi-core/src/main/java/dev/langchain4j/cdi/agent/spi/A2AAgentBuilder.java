package dev.langchain4j.cdi.agent.spi;

import jakarta.enterprise.inject.Instance;

/**
 * SPI for building A2A (Agent-to-Agent) agents. Implemented by the {@code langchain4j-cdi-a2a} module which brings in
 * the required {@code langchain4j-agentic-a2a} dependency.
 */
public interface A2AAgentBuilder {

    /**
     * Build an A2A agent from individual field values.
     *
     * <p>Delegates to {@link #build(Class, String, String, boolean, boolean, String, Instance)} with {@code optional}
     * set to {@code false}.
     *
     * @param <X> the agent interface type
     * @param interfaceClass the agent interface class
     * @param url the A2A server URL
     * @param outputKey the key under which the agent stores its output in the agentic scope, or blank to skip
     * @param async whether the agent executes asynchronously
     * @param agentListenerName CDI bean name of the {@link dev.langchain4j.agentic.observability.AgentListener}, or
     *     blank for none
     * @param lookup CDI lookup instance for resolving named beans
     * @return a proxy implementing {@code interfaceClass} that delegates to the remote A2A server
     */
    default <X> X build(
            Class<X> interfaceClass,
            String url,
            String outputKey,
            boolean async,
            String agentListenerName,
            Instance<Object> lookup) {
        return build(interfaceClass, url, outputKey, async, false, agentListenerName, lookup);
    }

    /**
     * Build an A2A agent from individual field values including the optional flag.
     *
     * @param <X> the agent interface type
     * @param interfaceClass the agent interface class
     * @param url the A2A server URL
     * @param outputKey the key under which the agent stores its output in the agentic scope, or blank to skip
     * @param async whether the agent executes asynchronously
     * @param optional when {@code true} the agent's execution is silently skipped if any of its arguments is missing in
     *     the agentic scope, instead of failing the entire agentic system
     * @param agentListenerName CDI bean name of the {@link dev.langchain4j.agentic.observability.AgentListener}, or
     *     blank for none
     * @param lookup CDI lookup instance for resolving named beans
     * @return a proxy implementing {@code interfaceClass} that delegates to the remote A2A server
     */
    <X> X build(
            Class<X> interfaceClass,
            String url,
            String outputKey,
            boolean async,
            boolean optional,
            String agentListenerName,
            Instance<Object> lookup);
}
