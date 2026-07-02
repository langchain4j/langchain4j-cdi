package dev.langchain4j.cdi.core.portableextension;

import jakarta.enterprise.inject.spi.BeanAttributes;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Extended {@link BeanAttributes} that exposes interceptor binding annotations for a bean.
 *
 * @param <T> the bean type
 * @author Buhake Sindi
 * @since 26 June 2026
 */
public interface InterceptorBeanAttributes<T> extends BeanAttributes<T> {

    /**
     * Obtains the {@code InterceptorBinding} annotations of the bean.
     *
     * @return the interceptor bindings
     */
    public Set<Annotation> getInterceptorBindings();
}
