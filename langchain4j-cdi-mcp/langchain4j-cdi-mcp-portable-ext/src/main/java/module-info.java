/**
 * Portable CDI extension that discovers MCP {@code @Tool}/{@code @Prompt}/{@code @Resource} beans at deployment time on
 * traditional Jakarta EE servers (WildFly, Payara, GlassFish, Liberty).
 */
open module dev.langchain4j.cdi.mcp.portable {
    requires transitive jakarta.cdi;
    requires dev.langchain4j.cdi.mcp.server;
    requires java.logging;
    requires mcp.server.api;

    provides jakarta.enterprise.inject.spi.Extension with
            dev.langchain4j.cdi.mcp.portableextension.McpServerPortableExtension;
}
