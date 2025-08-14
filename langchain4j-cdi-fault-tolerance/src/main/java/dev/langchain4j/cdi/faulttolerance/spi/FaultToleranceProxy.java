/**
 *
 */
package dev.langchain4j.cdi.faulttolerance.spi;

import java.lang.reflect.Proxy;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.InvocationContext;

/**
 * @author Buhake Sindi
 * @since 23 July 2025
 */
public class FaultToleranceProxy {

    @SuppressWarnings("unchecked")
    public static <T> T create(final Bean<T> bean, final BeanManager beanManager, final InvocationContext invocationContext) {
        T instance = getReference(bean, beanManager);
        return (T) Proxy.newProxyInstance(
                bean.getBeanClass().getClassLoader(),
                new Class<?>[] { bean.getBeanClass() },
                new FaultToleranceInvocationHandler(instance, beanManager, invocationContext));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getReference(Bean<T> bean, BeanManager beanManager) {
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, bean.getBeanClass(), context);
    }
}
