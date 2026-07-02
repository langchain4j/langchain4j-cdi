package dev.langchain4j.cdi.example.chat;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import jakarta.enterprise.context.ApplicationScoped;

/** AI assistant interface for chat interactions with session memory. */
@RegisterAIService(
        chatModelName = "chat-assistant",
        scope = ApplicationScoped.class,
        chatMemoryProviderName = "#default")
interface Assistant {

    /**
     * Sends a message to the assistant within a session.
     *
     * @param sessionId the session identifier for memory
     * @param userMessage the user message
     * @return the assistant's response
     */
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
