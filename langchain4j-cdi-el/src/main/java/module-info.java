/**
 * Jakarta EL {@code ExpressionResolver} implementation for LangChain4j CDI: evaluates {@code #{...}} expressions.
 *
 * <p>Registered via {@link java.util.ServiceLoader} as a provider of
 * {@code dev.langchain4j.cdi.spi.ExpressionResolver}.
 */
open module dev.langchain4j.cdi.el {
    requires dev.langchain4j.cdi.core;
    requires jakarta.cdi;
    requires jakarta.el;
    requires java.logging;

    provides dev.langchain4j.cdi.spi.ExpressionResolver with
            dev.langchain4j.cdi.el.JakartaELExpressionResolver;
}
