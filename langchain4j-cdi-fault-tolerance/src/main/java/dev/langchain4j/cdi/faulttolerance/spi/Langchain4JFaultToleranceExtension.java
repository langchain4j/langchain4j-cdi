package dev.langchain4j.cdi.faulttolerance.spi;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import dev.langchain4j.cdi.core.portableextension.LangChain4JAIServiceBean;
import dev.langchain4j.cdi.faulttolerance.spi.ApplyFaultTolerance.ApplyFaultToleranceLiteral;
import dev.langchain4j.cdi.spi.RegisterAIService;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessSyntheticBean;

/**
 * @author Buhake Sindi
 * @since 29 November 2024
 */
public class Langchain4JFaultToleranceExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(Langchain4JFaultToleranceExtension.class.getName());

    private static final Set<Class<? extends Annotation>> FAULT_TOLERANCE_ANNOTATIONS = Set.of(Retry.class,
            CircuitBreaker.class,
            Bulkhead.class,
            Timeout.class,
            Asynchronous.class,
            Fallback.class);

    void registerInterceptorBindings(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addInterceptorBinding(bm.createAnnotatedType(ApplyFaultTolerance.class));
        bbd.addAnnotatedType(bm.createAnnotatedType(ApplyFaultToleranceInterceptor.class),
                ApplyFaultToleranceInterceptor.class.getName());
    }

    <X> void applyFaultTolerance(@Observes ProcessSyntheticBean<X> event, BeanManager bm) {
        Bean<X> bean = event.getBean();
        if (bean.getStereotypes().contains(RegisterAIService.class)) {
            AnnotatedType<?> annotatedType = bm.createAnnotatedType(event.getBean().getBeanClass());
            for (AnnotatedMethod<?> annotatedMethod : annotatedType.getMethods()) {
                if (isFaultToleranceMethod(annotatedMethod)) {
                    LOGGER.info("applyFaultTolerance: Synthetic Bean of type -> " + bean.getBeanClass());

                    //Add Fault Tolerance interceptor Binding
                    ((LangChain4JAIServiceBean<X>) bean).getInterceptorBindings().add(new ApplyFaultToleranceLiteral());
                }
            }
        }
    }

    boolean isFaultToleranceMethod(AnnotatedMethod<?> annotatedMethod) {

        return FAULT_TOLERANCE_ANNOTATIONS.stream()
                .anyMatch(annotationClass -> annotatedMethod.isAnnotationPresent(annotationClass));
    }
}
