package dev.langchain4j.cdi.faulttolerance.spi;

import dev.langchain4j.cdi.spi.RegisterSimpleAgent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

// @SuppressWarnings: CDI proxy is created by the portable extension, not by the container directly
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterSimpleAgent(chatModelName = "#default", scope = ApplicationScoped.class)
public interface BulkheadAgentService {

    @Bulkhead(5)
    @UserMessage("{question}")
    String chat(@V("question") String question);
}
