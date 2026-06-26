package dev.langchain4j.cdi.faulttolerance.spi;

import dev.langchain4j.cdi.core.portableextension.InterceptorBeanAttributes;
import dev.langchain4j.cdi.core.portableextension.LangChain4JAIServicePortableExtension;
import dev.langchain4j.cdi.core.portableextension.LangChain4JPluginsPortableExtension;
import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.Set;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
class FaultToleranceAgentExtensionTest {

    @Inject
    BeanManager beanManager;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
                    LangChain4JAIServicePortableExtension.class,
                    LangChain4JPluginsPortableExtension.class,
                    Langchain4JFaultToleranceExtension.class,
                    RetryAgentService.class,
                    RetrySequenceAgentService.class,
                    TimeoutAgentService.class,
                    BulkheadAgentService.class,
                    FallbackAgentService.class,
                    AsynchronousAgentService.class,
                    PlainAgentService.class,
                    DummyChatModel.class,
                    ConfigExtension.class)
            .build();

    private void assertFaultToleranceBinding(Class<?> serviceClass, boolean expected, String message) {
        Bean<?> bean = beanManager.getBeans(serviceClass).iterator().next();
        Assertions.assertInstanceOf(InterceptorBeanAttributes.class, bean);

        Set<Annotation> bindings = ((InterceptorBeanAttributes<?>) bean).getInterceptorBindings();
        boolean has = bindings.stream().anyMatch(a -> a.annotationType() == ApplyFaultTolerance.class);
        Assertions.assertEquals(expected, has, message);
    }

    @Test
    void agentWithRetryGetsInterceptorBinding() {
        assertFaultToleranceBinding(
                RetryAgentService.class, true, "@RegisterSimpleAgent with @Retry must get @ApplyFaultTolerance");
    }

    @Test
    void sequenceAgentWithCircuitBreakerGetsInterceptorBinding() {
        assertFaultToleranceBinding(
                RetrySequenceAgentService.class,
                true,
                "@RegisterSequenceAgent with @CircuitBreaker must get @ApplyFaultTolerance");
    }

    @Test
    void agentWithTimeoutGetsInterceptorBinding() {
        assertFaultToleranceBinding(
                TimeoutAgentService.class, true, "@RegisterSimpleAgent with @Timeout must get @ApplyFaultTolerance");
    }

    @Test
    void agentWithBulkheadGetsInterceptorBinding() {
        assertFaultToleranceBinding(
                BulkheadAgentService.class, true, "@RegisterSimpleAgent with @Bulkhead must get @ApplyFaultTolerance");
    }

    @Test
    void agentWithFallbackGetsInterceptorBinding() {
        assertFaultToleranceBinding(
                FallbackAgentService.class, true, "@RegisterSimpleAgent with @Fallback must get @ApplyFaultTolerance");
    }

    @Test
    void agentWithAsynchronousGetsInterceptorBinding() {
        assertFaultToleranceBinding(
                AsynchronousAgentService.class,
                true,
                "@RegisterSimpleAgent with @Asynchronous must get @ApplyFaultTolerance");
    }

    @Test
    void agentWithoutFaultToleranceAnnotationsGetsNoBinding() {
        assertFaultToleranceBinding(
                PlainAgentService.class,
                false,
                "@RegisterSimpleAgent without fault tolerance annotations must not get @ApplyFaultTolerance");
    }
}
