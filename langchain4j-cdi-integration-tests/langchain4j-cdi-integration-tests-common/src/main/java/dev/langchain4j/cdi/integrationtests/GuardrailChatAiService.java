package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;

/** AI chat service with guardrails for integration testing. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterAIService(
        chatModelName = "chat-model",
        inputGuardrails = {NoEmptyMessageInputGuardrail.class},
        outputGuardrails = {PassThroughOutputGuardrail.class})
public interface GuardrailChatAiService {

    /**
     * Sends a question to the guarded chat model.
     *
     * @param question the user question
     * @return the model response
     */
    @SystemMessage("""
            my system message.
            """)
    String chat(String question);
}
