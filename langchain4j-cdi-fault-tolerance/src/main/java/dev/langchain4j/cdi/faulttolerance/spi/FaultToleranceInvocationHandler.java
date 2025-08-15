/**
 *
 */
package dev.langchain4j.cdi.faulttolerance.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Buhake Sindi
 * @since 23 July 2025
 */
public class FaultToleranceInvocationHandler implements InvocationHandler {

    private static final Set<Class<? extends Annotation>> MICROPROFILE_FAULT_TOLERANCE_ANNOTATIONS = Set.of(Retry.class,
            CircuitBreaker.class,
            Bulkhead.class,
            Timeout.class,
            Asynchronous.class,
            Fallback.class);

    private final Object target;
    private final BeanManager beanManager;
    private final InvocationContext invocationContext;

    public FaultToleranceInvocationHandler(final Object target, final BeanManager beanManager,
            final InvocationContext invocationContext) {
        this.target = target;
        this.beanManager = beanManager;
        this.invocationContext = invocationContext;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Resolve interceptors based on annotations on the method
        final InterceptionType interception = InterceptionType.AROUND_INVOKE;
        List<Annotation> annotations = MICROPROFILE_FAULT_TOLERANCE_ANNOTATIONS.stream()
                .map(annotationClass -> method.getAnnotation(annotationClass)).filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(interception,
                annotations.toArray(new Annotation[annotations.size()]));

        if (interceptors.isEmpty()) {
            return method.invoke(target, args);
        }

        return new InterceptorChain(target, method, args, interception, interceptors).invoke(invocationContext, beanManager);
    }

    class InterceptorChain {
        private final Object target;
        private final Method method;
        private final Object[] args;
        private final InterceptionType interceptionType;
        private final List<Interceptor<?>> interceptors;
        private int position = 0;

        InterceptorChain(Object target, Method method, Object[] args, InterceptionType interceptionType,
                List<Interceptor<?>> interceptors) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.interceptionType = interceptionType;
            this.interceptors = interceptors;
        }

        Object invoke(final InvocationContext invocationContext, final BeanManager bm) throws Exception {
            if (position < interceptors.size()) {
                @SuppressWarnings("unchecked")
                Interceptor<Object> interceptor = (Interceptor<Object>) interceptors.get(position++);
                CreationalContext<?> context = beanManager.createCreationalContext(interceptor);
                Object interceptorInstance = beanManager.getReference(interceptor, interceptor.getBeanClass(), context);
                return interceptor.intercept(interceptionType, interceptorInstance,
                        createInvocationContext(invocationContext, bm));
            }
            return method.invoke(target, args);
        }

        private InvocationContext createInvocationContext(final InvocationContext invocationContext, final BeanManager bm) {
            return new InvocationContext() {

                @Override
                public Object getTarget() {
                    return target;
                }

                @Override
                public Object getTimer() {
                    return invocationContext.getTimer();
                }

                @Override
                public Method getMethod() {
                    // TODO Auto-generated method stub
                    return method;
                }

                @Override
                public Constructor<?> getConstructor() {
                    return invocationContext.getConstructor();
                }

                @Override
                public Object[] getParameters() {
                    return args;
                }

                @Override
                public void setParameters(Object[] params) {
                    // TODO Auto-generated method stub
                    invocationContext.setParameters(params);
                }

                @Override
                public Map<String, Object> getContextData() {
                    return invocationContext.getContextData();
                }

                @Override
                public Object proceed() throws Exception {
                    return InterceptorChain.this.invoke(invocationContext, beanManager);
                }
            };
        }
    }
}
