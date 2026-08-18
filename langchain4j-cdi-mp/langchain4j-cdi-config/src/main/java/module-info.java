/**
 * MicroProfile Config integration for LangChain4j CDI: exposes external configuration as {@code LLMConfig}, resolves
 * {@code ${...}} property expressions and converts {@code Duration} values.
 *
 * <p>Contributes the {@code LLMConfig}, {@code ExpressionResolver} and MicroProfile {@code Converter} SPIs via
 * {@code provides} (the {@code META-INF/services} files are kept for class-path discovery).
 */
open module dev.langchain4j.cdi.mp.config {
    requires dev.langchain4j.cdi.core;
    requires transitive org.eclipse.microprofile.config;
    requires java.logging;

    provides dev.langchain4j.cdi.core.config.spi.LLMConfig with
            dev.langchain4j.cdi.core.mpconfig.LLMConfigMPConfig;
    provides dev.langchain4j.cdi.spi.ExpressionResolver with
            dev.langchain4j.cdi.core.mpconfig.MpConfigExpressionResolver;
    provides org.eclipse.microprofile.config.spi.Converter with
            dev.langchain4j.cdi.core.mpconfig.DurationConverter;
}
