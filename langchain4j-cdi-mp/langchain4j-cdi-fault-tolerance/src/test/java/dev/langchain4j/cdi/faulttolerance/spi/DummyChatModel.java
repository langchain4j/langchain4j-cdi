package dev.langchain4j.cdi.faulttolerance.spi;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DummyChatModel implements ChatModel {

    @Override
    public String chat(String userMessage) {
        return "dummy";
    }
}
