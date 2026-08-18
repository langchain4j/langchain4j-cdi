/**
 * Portable CDI extension for LangChain4j: registers {@code @RegisterAIService} interfaces and configured plugin beans
 * on traditional Jakarta EE servers (WildFly, GlassFish, Liberty, Payara).
 *
 * <p>The two {@code Extension} implementations are contributed via {@code provides} so they are discovered on the
 * module path as well as on the class path (the {@code META-INF/services} file is kept for the class-path case).
 */
open module dev.langchain4j.cdi.portable {
    requires transitive dev.langchain4j.cdi.core;
    requires jakarta.cdi;
    requires java.logging;
    // agent topologies rely on the optional langchain4j-agentic dependency
    requires static langchain4j.agentic;

    exports dev.langchain4j.cdi.core.portableextension;

    provides jakarta.enterprise.inject.spi.Extension with
            dev.langchain4j.cdi.core.portableextension.LangChain4JPluginsPortableExtension,
            dev.langchain4j.cdi.core.portableextension.LangChain4JAIServicePortableExtension;
}
