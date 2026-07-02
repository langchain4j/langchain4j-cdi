package dev.langchain4j.cdi.faulttolerance.spi;

import dev.langchain4j.cdi.core.portableextension.InterceptorBeanAttributes;
import dev.langchain4j.cdi.faulttolerance.spi.ApplyFaultTolerance.ApplyFaultToleranceLiteral;
import dev.langchain4j.cdi.spi.RegisterA2AAgent;
import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.cdi.spi.RegisterConditionalAgent;
import dev.langchain4j.cdi.spi.RegisterHumanInTheLoopAgent;
import dev.langchain4j.cdi.spi.RegisterLoopAgent;
import dev.langchain4j.cdi.spi.RegisterMcpClientAgent;
import dev.langchain4j.cdi.spi.RegisterParallelAgent;
import dev.langchain4j.cdi.spi.RegisterParallelMapperAgent;
import dev.langchain4j.cdi.spi.RegisterPlannerAgent;
import dev.langchain4j.cdi.spi.RegisterSequenceAgent;
import dev.langchain4j.cdi.spi.RegisterSimpleAgent;
import dev.langchain4j.cdi.spi.RegisterSupervisorAgent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessSyntheticBean;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Logger;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * CDI portable extension that applies MicroProfile Fault Tolerance interceptor bindings to AI service synthetic beans.
 *
 * @author Buhake Sindi
 * @since 29 November 2024
 */
public class Langchain4JFaultToleranceExtension implements Extension {

    /** Creates a new {@code Langchain4JFaultToleranceExtension}. */
    public Langchain4JFaultToleranceExtension() {}

    private static final Logger LOGGER = Logger.getLogger(Langchain4JFaultToleranceExtension.class.getName());

    static final Set<Class<? extends Annotation>> AI_STEREOTYPES_ANNOTATIONS = Set.of(
            RegisterAIService.class,
            RegisterSimpleAgent.class,
            RegisterSequenceAgent.class,
            RegisterLoopAgent.class,
            RegisterParallelAgent.class,
            RegisterParallelMapperAgent.class,
            RegisterConditionalAgent.class,
            RegisterSupervisorAgent.class,
            RegisterPlannerAgent.class,
            RegisterA2AAgent.class,
            RegisterMcpClientAgent.class,
            RegisterHumanInTheLoopAgent.class);
    static final Set<Class<? extends Annotation>> FAULT_TOLERANCE_ANNOTATIONS = Set.of(
            Retry.class, CircuitBreaker.class, Bulkhead.class, Timeout.class, Asynchronous.class, Fallback.class);

    void registerInterceptorBindings(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addInterceptorBinding(bm.createAnnotatedType(ApplyFaultTolerance.class));
        bbd.addAnnotatedType(
                bm.createAnnotatedType(ApplyFaultToleranceInterceptor.class),
                ApplyFaultToleranceInterceptor.class.getName());
    }

    <X> void applyFaultTolerance(@Observes ProcessSyntheticBean<X> event, BeanManager bm) {
        Bean<X> bean = event.getBean();
        if (isBeanAiAnnotated(bean)) {
            AnnotatedType<?> annotatedType =
                    bm.createAnnotatedType(event.getBean().getBeanClass());
            for (AnnotatedMethod<?> annotatedMethod : annotatedType.getMethods()) {
                if (isFaultToleranceMethod(annotatedMethod)) {
                    LOGGER.info("applyFaultTolerance: Synthetic Bean of type -> " + bean.getBeanClass());

                    // Add Fault Tolerance interceptor Binding
                    if (bean instanceof InterceptorBeanAttributes<?> iba)
                        iba.getInterceptorBindings().add(new ApplyFaultToleranceLiteral());
                }
            }
        }
    }

    boolean isBeanAiAnnotated(final Bean<?> bean) {
        return AI_STEREOTYPES_ANNOTATIONS.stream().anyMatch(bean.getStereotypes()::contains);
    }

    boolean isFaultToleranceMethod(AnnotatedMethod<?> annotatedMethod) {
        return FAULT_TOLERANCE_ANNOTATIONS.stream()
                .anyMatch(annotationClass -> annotatedMethod.isAnnotationPresent(annotationClass));
    }
}
