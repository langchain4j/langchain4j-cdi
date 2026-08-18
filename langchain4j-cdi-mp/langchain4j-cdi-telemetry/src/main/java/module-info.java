/**
 * MicroProfile Telemetry (OpenTelemetry) integration for LangChain4j CDI: chat-model and agent listeners that emit
 * spans and metrics following the GenAI semantic conventions.
 *
 * <p>All types are CDI beans discovered by the container; declared as an {@code open module} for runtime reflection.
 */
open module dev.langchain4j.cdi.mp.telemetry {
    requires dev.langchain4j.cdi.core;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires jakarta.cdi;
    requires jakarta.annotation;
    requires java.logging;
    // agent observability relies on the optional langchain4j-agentic dependency
    requires static langchain4j.agentic;
}
