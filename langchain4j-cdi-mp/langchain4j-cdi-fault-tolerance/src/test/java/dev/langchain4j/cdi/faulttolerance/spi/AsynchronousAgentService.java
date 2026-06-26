package dev.langchain4j.cdi.faulttolerance.spi;

import dev.langchain4j.cdi.spi.RegisterSimpleAgent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.faulttolerance.Asynchronous;

// @SuppressWarnings: CDI proxy is created by the portable extension, not by the container directly
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterSimpleAgent(chatModelName = "#default", scope = ApplicationScoped.class)
public interface AsynchronousAgentService {

    @Asynchronous
    @UserMessage("{question}")
    CompletionStage<String> chat(@V("question") String question);
}
