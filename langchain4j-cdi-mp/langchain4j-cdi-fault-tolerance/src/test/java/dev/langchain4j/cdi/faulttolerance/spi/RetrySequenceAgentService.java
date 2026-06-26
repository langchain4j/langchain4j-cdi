package dev.langchain4j.cdi.faulttolerance.spi;

import dev.langchain4j.cdi.spi.RegisterSequenceAgent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

// @SuppressWarnings: CDI proxy is created by the portable extension, not by the container directly
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterSequenceAgent(scope = ApplicationScoped.class)
public interface RetrySequenceAgentService {

    @CircuitBreaker(requestVolumeThreshold = 4)
    String run(String input);
}
