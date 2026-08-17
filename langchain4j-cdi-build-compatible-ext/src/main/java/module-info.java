/**
 * Build-compatible CDI extension for LangChain4j: discovers {@code @RegisterAIService}, agent and plugin beans at build
 * time for ahead-of-time frameworks (Quarkus, Helidon).
 *
 * <p>The two {@code BuildCompatibleExtension} implementations are contributed via {@code provides}; the pluggable
 * {@code AISyntheticBeanCreatorClassFactory} SPI is consumed via {@code uses}.
 */
open module dev.langchain4j.cdi.buildcompatible {
    requires dev.langchain4j.cdi.core;
    requires jakarta.cdi;
    requires jakarta.annotation;
    // agent topologies rely on the optional langchain4j-agentic dependency
    requires static langchain4j.agentic;

    uses dev.langchain4j.cdi.core.buildcompatibleextension.AISyntheticBeanCreatorClassFactory;

    provides jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension with
            dev.langchain4j.cdi.core.buildcompatibleextension.Langchain4JAIServiceBuildCompatibleExtension,
            dev.langchain4j.cdi.core.buildcompatibleextension.LangChain4JPluginsBuildCompatibleExtension;
}
