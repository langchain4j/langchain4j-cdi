package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.cdi.spi.RegisterAIService;

/** AI service that uses a named listener to capture thinking. */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterAIService(
        chatModelName = "thinking-model",
        listenerNames = {ThinkingListener.BEAN_NAME})
public interface ListenerChatAiService {

    String chat(String question);
}
