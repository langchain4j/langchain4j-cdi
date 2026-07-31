package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.concurrent.atomic.AtomicReference;

/** Named CDI bean that captures thinking text from AI service responses. */
@ApplicationScoped
@Named(ThinkingListener.BEAN_NAME)
public class ThinkingListener implements AiServiceResponseReceivedListener {

    public static final String BEAN_NAME = "thinkingListener";

    private final AtomicReference<String> lastThinking = new AtomicReference<>();

    public ThinkingListener() {}

    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
        String thinking = event.response().aiMessage().thinking();
        if (thinking != null && !thinking.isBlank()) {
            lastThinking.set(thinking);
        }
    }

    public String getLastThinking() {
        return lastThinking.get();
    }

    public void reset() {
        lastThinking.set(null);
    }
}
