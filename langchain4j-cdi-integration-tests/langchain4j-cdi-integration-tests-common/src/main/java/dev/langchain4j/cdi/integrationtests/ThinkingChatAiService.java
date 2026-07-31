package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.cdi.aiservice.OnThinking;
import dev.langchain4j.cdi.aiservice.ThinkingEmitted;
import dev.langchain4j.cdi.spi.RegisterAIService;
import java.util.concurrent.atomic.AtomicReference;

/** AI service that captures thinking via {@link OnThinking}. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterAIService(chatModelName = "thinking-model")
public interface ThinkingChatAiService {

    AtomicReference<String> LAST_THINKING = new AtomicReference<>();

    String chat(String question);

    @OnThinking
    static void onThinking(ThinkingEmitted event) {
        LAST_THINKING.set(event.text());
    }
}
