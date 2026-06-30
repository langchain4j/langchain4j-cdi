package dev.langchain4j.cdi.example.chat;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

/** CDI bean managing the chat agent and its memory provider. */
@ApplicationScoped
public class ChatAgent {

    /** Creates a new ChatAgent. */
    public ChatAgent() {}

    private static final String CHAT_MEMORY_CDI_NAME = "chat-ai-service-memory";

    @Produces
    private ChatMemoryProvider chatMemoryProvider;

    @PostConstruct
    private void init() {
        chatMemoryProvider = sessionId -> CDI.current()
                .select(ChatMemory.class, NamedLiteral.of(CHAT_MEMORY_CDI_NAME))
                .get();
    }

    @Inject
    private Assistant assistant = null;

    /**
     * Returns the assistant instance.
     *
     * @return the assistant
     */
    public Assistant getAssistant() {
        if (assistant == null) {
            assistant = CDI.current().select(Assistant.class).get();
        }
        return assistant;
    }

    /**
     * Sends a message to the assistant within a session.
     *
     * @param sessionId the session identifier
     * @param message the user message
     * @return the assistant's response
     */
    public String chat(String sessionId, String message) {
        String reply = getAssistant().chat(sessionId, message).trim();
        int i = reply.lastIndexOf(message);
        return i > 0 ? reply.substring(i) : reply;
    }
}
