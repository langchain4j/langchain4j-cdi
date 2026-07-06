/**
 * MCP (Model Context Protocol) server runtime for LangChain4j CDI: exposes {@code @Tool}/{@code @Prompt}/
 * {@code @Resource} CDI beans over JSON-RPC 2.0 / Streamable HTTP at the {@code /mcp} endpoint.
 *
 * <p>Declared as an {@code open module} so the CDI container and JAX-RS runtime can reflectively access the endpoint,
 * registries and transport handlers. The {@code registry} package is exported for the MCP discovery extensions.
 */
open module dev.langchain4j.cdi.mcp.server {
    requires transitive jakarta.cdi;
    requires transitive mcp.annotations;
    requires jakarta.inject;
    requires jakarta.json;
    requires jakarta.ws.rs;
    requires mcp.model;
    requires mcp.server.api;
    requires jakarta.annotation;
    requires jakarta.json.bind;
    requires java.logging;

    exports dev.langchain4j.cdi.mcp.server.registry;
}
