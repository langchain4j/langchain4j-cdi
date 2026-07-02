package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.cdi.spi.RegisterSimpleAgent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import jakarta.enterprise.context.ApplicationScoped;

/** Simple agent AI service for integration testing. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterSimpleAgent(chatModelName = "chat-model", scope = ApplicationScoped.class)
public interface AgentChatService {

    /**
     * Sends a question to the agent.
     *
     * @param question the user question
     * @return the agent response
     */
    @SystemMessage("You are a helpful assistant.")
    @UserMessage("{question}")
    String chat(@V("question") String question);
}
