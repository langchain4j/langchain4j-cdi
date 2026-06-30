package dev.langchain4j.cdi.guardrail;

import dev.langchain4j.service.guardrail.GuardrailService;
import dev.langchain4j.service.guardrail.spi.GuardrailServiceBuilderFactory;

/**
 * A CDI-aware implementation of {@link GuardrailServiceBuilderFactory} that creates {@link GuardrailService.Builder}
 * instances which resolve guardrail classes as CDI beans.
 *
 * <p>This factory is registered via the {@link java.util.ServiceLoader} mechanism and is automatically picked up by
 * {@link GuardrailService#builder(Class)} when present on the classpath.
 *
 * <p>When guardrail classes are referenced in {@code @InputGuardrails} or {@code @OutputGuardrails} annotations, the
 * resulting builder will look them up as CDI managed beans rather than instantiating them via reflection, allowing
 * guardrails to benefit from CDI injection, interceptors, and lifecycle management.
 */
public class CdiGuardrailServiceBuilderFactory implements GuardrailServiceBuilderFactory {

    /** Creates a new factory instance. */
    public CdiGuardrailServiceBuilderFactory() {}

    @Override
    public GuardrailService.Builder getBuilder(Class<?> aiServiceClass) {
        return new CdiGuardrailServiceBuilder(aiServiceClass);
    }
}
