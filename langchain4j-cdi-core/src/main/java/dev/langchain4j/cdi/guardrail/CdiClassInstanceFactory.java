package dev.langchain4j.cdi.guardrail;

import dev.langchain4j.spi.classloading.ClassInstanceFactory;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CDI-aware {@link ClassInstanceFactory} that resolves the instances LangChain4j creates from classes, such as the
 * guardrail classes referenced in {@code @InputGuardrails} or {@code @OutputGuardrails} annotations, as CDI managed
 * beans.
 *
 * <p>This allows those instances to benefit from CDI injection, interceptors, decorators and scoping. If the class is
 * not a resolvable CDI bean, or if no CDI container is available, an instance is created with the class's no-arg
 * constructor, as LangChain4j does by default.
 *
 * <p>This factory is registered via the {@link java.util.ServiceLoader} mechanism, and is picked up by LangChain4j when
 * present on the classpath. With it, the default guardrail service builder is all that is needed.
 */
public class CdiClassInstanceFactory implements ClassInstanceFactory {
    private static final Logger LOGGER = Logger.getLogger(CdiClassInstanceFactory.class.getName());

    /** Creates a new factory instance. */
    public CdiClassInstanceFactory() {}

    @Override
    public <T> T getInstanceOfClass(Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz");

        return resolveFromCdi(clazz).orElseGet(() -> instantiate(clazz));
    }

    private static <T> Optional<T> resolveFromCdi(Class<T> clazz) {
        try {
            var instance = CDI.current().select(clazz);

            return instance.isResolvable() ? Optional.ofNullable(instance.get()) : Optional.empty();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.FINE, "CDI container not available, falling back to reflection for " + clazz.getName(), e);

            return Optional.empty();
        }
    }

    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create an instance of " + clazz.getName(), e);
        }
    }
}
