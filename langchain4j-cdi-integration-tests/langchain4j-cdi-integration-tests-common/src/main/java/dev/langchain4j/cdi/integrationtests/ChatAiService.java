package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@RegisterAIService(chatModelName = "chat-model")
public interface ChatAiService {

    @SystemMessage("""
            my system message.
            """)
    String chat(String question);

}
