/**
 * MicroProfile Fault Tolerance integration for LangChain4j CDI: enables {@code @Retry}, {@code @Timeout},
 * {@code @CircuitBreaker} and {@code @Fallback} on AI service methods.
 *
 * <p>Registers its CDI portable {@code Extension} via {@code provides} (the {@code META-INF/services} file is kept for
 * class-path discovery).
 */
open module dev.langchain4j.cdi.mp.faulttolerance {
    requires jakarta.cdi;
    requires dev.langchain4j.cdi.core;
    requires dev.langchain4j.cdi.portable;
    requires jakarta.annotation;
    requires jakarta.inject;
    requires jakarta.interceptor;
    requires java.logging;
    requires microprofile.fault.tolerance.api;

    provides jakarta.enterprise.inject.spi.Extension with
            dev.langchain4j.cdi.faulttolerance.spi.Langchain4JFaultToleranceExtension;
}
