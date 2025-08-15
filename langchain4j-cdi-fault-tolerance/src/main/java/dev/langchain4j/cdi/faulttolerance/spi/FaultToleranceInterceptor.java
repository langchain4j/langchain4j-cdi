/**
 *
 */
package dev.langchain4j.cdi.faulttolerance.spi;

import java.lang.reflect.Proxy;

import dev.langchain4j.cdi.spi.RegisterAIService;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
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
public class FaultToleranceInterceptor {

    @Inject
    private BeanManager beanManager;

    @Inject
    @Intercepted
    private Bean<?> interceptedBean;

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Throwable {

        if (interceptedBean.getBeanClass().isAnnotationPresent(RegisterAIService.class)) {
            Object proxy = FaultToleranceProxy.create(interceptedBean, beanManager, invocationContext);
            return Proxy.getInvocationHandler(proxy).invoke(proxy, invocationContext.getMethod(),
                    invocationContext.getParameters());
        }

        return invocationContext.proceed();
    }
}
