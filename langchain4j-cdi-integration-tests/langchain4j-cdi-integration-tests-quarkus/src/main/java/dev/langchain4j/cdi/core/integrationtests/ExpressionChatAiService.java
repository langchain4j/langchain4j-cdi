package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;
import jakarta.enterprise.context.ApplicationScoped;

/** AI chat service using expression-resolved model name for integration testing. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterAIService(chatModelName = "${test.expression.model}", scope = ApplicationScoped.class)
public interface ExpressionChatAiService {

    /**
     * Sends a question to the chat model.
     *
     * @param question the user question
     * @return the model response
     */
    @SystemMessage("my system message.")
    String chat(String question);
}
