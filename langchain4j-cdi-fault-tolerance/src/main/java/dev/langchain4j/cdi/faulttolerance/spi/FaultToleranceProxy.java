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

    //    @SuppressWarnings("unchecked")
    //    public static <T> T create(final Class<T> aiServiceClass, final BeanManager beanManager, final InvocationContext invocationContext) {
    //        T instance = getBean(aiServiceClass, beanManager);
    //        return (T) Proxy.newProxyInstance(
    //                aiServiceClass.getClassLoader(),
    //                new Class<?>[] { aiServiceClass },
    //                new FaultToleranceInvocationHandler(instance, beanManager, invocationContext));
    //    }

    @SuppressWarnings("unchecked")
    public static <T> T create(final Bean<T> bean, final BeanManager beanManager, final InvocationContext invocationContext) {
        T instance = getBean(bean, beanManager);
        return (T) Proxy.newProxyInstance(
                bean.getBeanClass().getClassLoader(),
                new Class<?>[] { bean.getBeanClass() },
                new FaultToleranceInvocationHandler(instance, beanManager, invocationContext));
    }

    //    @SuppressWarnings("unchecked")
    //    private static <T> T getBean(Class<T> beanType, BeanManager beanManager) {
    //        // The trick: find the ORIGINAL bean that is NOT an alternative
    //        Set<Bean<?>> candidates = beanManager.getBeans(beanType); // beanManager.getBeans(originalBean.getBeanClass(), originalBean.getQualifiers().toArray(new Annotation[0]));
    //        Bean<?> nonAlternativeBean = candidates.stream()
    //                .filter(b -> !b.isAlternative())
    //                .findFirst()
    //                .orElseThrow(() -> new IllegalStateException("Could not find original bean to proxy for " + beanType));
    //
    //        //        CreationalContext<?> context = beanManager.createCreationalContext(nonAlternativeBean);
    //        //        return (T) beanManager.getReference(nonAlternativeBean, beanType, context);
    //        return (T) getBean(nonAlternativeBean, beanManager);
    //    }

    @SuppressWarnings("unchecked")
    private static <T> T getBean(Bean<T> bean, BeanManager beanManager) {
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, bean.getBeanClass(), context);
    }
}
