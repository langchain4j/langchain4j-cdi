/**
 *
 */
package dev.langchain4j.cdi.faulttolerance.spi;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import dev.langchain4j.cdi.spi.RegisterAIService;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Buhake Sindi
 * @since 24 July 2025
 */
@Interceptor
@ApplyFaultTolerance
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 100)
class ApplyFaultToleranceInterceptor {

    private static final Set<Class<? extends Annotation>> MICROPROFILE_FAULT_TOLERANCE_ANNOTATIONS = Set.of(Retry.class,
            CircuitBreaker.class,
            Bulkhead.class,
            Timeout.class,
            Asynchronous.class,
            Fallback.class);

    @Inject
    private BeanManager beanManager;

    @Inject
    @Intercepted
    private Bean<?> interceptedBean;

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Throwable {

        if (interceptedBean.getBeanClass().isAnnotationPresent(RegisterAIService.class)) {
            final InterceptionType interception = InterceptionType.AROUND_INVOKE;
            List<Annotation> annotations = MICROPROFILE_FAULT_TOLERANCE_ANNOTATIONS.stream()
                    .map(annotationClass -> invocationContext.getMethod().getAnnotation(annotationClass))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<jakarta.enterprise.inject.spi.Interceptor<?>> interceptors = beanManager.resolveInterceptors(interception,
                    annotations.toArray(new Annotation[annotations.size()]));

            if (!interceptors.isEmpty()) {
                return new FaultToleranceInterceptorHandler(beanManager, interception, interceptors).handle(invocationContext);
            }
        }

        return invocationContext.proceed();
    }
}
