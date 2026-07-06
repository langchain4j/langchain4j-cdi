/**
 * LangChain4j CDI core module: AI service and agent stereotypes, the configuration/plugin creators and the pluggable
 * SPIs ({@code ExpressionResolver}, {@code A2AAgentBuilder}, {@code LLMConfig}).
 *
 * <p>Declared as an {@code open module} so the CDI container (Weld and friends) can reflectively access and proxy the
 * beans and configuration classes at runtime.
 */
open module dev.langchain4j.cdi.core {
    // Jakarta / JDK modules
    requires transitive jakarta.cdi;
    requires jakarta.annotation;
    requires java.naming;
    requires java.net.http;
    requires transitive java.logging;

    // LangChain4j (automatic modules — names derived from the jar file names)
    requires transitive langchain4j;
    requires transitive langchain4j.core;
    // langchain4j-agentic is an optional dependency (agent topologies) -> static (compile-time, optional at
    // runtime). Not transitive: consumers that actually use the agent API add their own requires.
    requires static langchain4j.agentic;
    requires langchain4j.http.client;

    // Public API
    exports dev.langchain4j.cdi.agent;
    exports dev.langchain4j.cdi.agent.spi;
    exports dev.langchain4j.cdi.aiservice;
    exports dev.langchain4j.cdi.core.config.spi;
    exports dev.langchain4j.cdi.core.http;
    exports dev.langchain4j.cdi.guardrail;
    exports dev.langchain4j.cdi.plugin;
    exports dev.langchain4j.cdi.spi;

    // SPIs consumed by this module via java.util.ServiceLoader
    uses dev.langchain4j.cdi.spi.ExpressionResolver;
    uses dev.langchain4j.cdi.agent.spi.A2AAgentBuilder;
    uses dev.langchain4j.cdi.core.config.spi.LLMConfig;

    // SPI implementation contributed to LangChain4j
    provides dev.langchain4j.service.guardrail.spi.GuardrailServiceBuilderFactory with
            dev.langchain4j.cdi.guardrail.CdiGuardrailServiceBuilderFactory;
}
