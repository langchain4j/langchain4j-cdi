/**
 * Build-compatible CDI extension that discovers MCP {@code @Tool}/{@code @Prompt}/{@code @Resource} beans at build time
 * for ahead-of-time frameworks (Quarkus, Helidon).
 */
open module dev.langchain4j.cdi.mcp.buildcompatible {
    requires transitive jakarta.cdi;
    requires dev.langchain4j.cdi.mcp.server;
    requires jakarta.cdi.lang.model;
    requires java.logging;
    requires mcp.annotations;

    provides jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension with
            dev.langchain4j.cdi.mcp.buildcompatible.McpServerBuildCompatibleExtension;
}
