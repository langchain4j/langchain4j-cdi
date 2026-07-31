package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

/** Mock {@link ChatModel} that always returns a response with thinking content. */
public class ThinkingChatModelMock implements ChatModel {

    /** The fixed thinking text returned by this mock. */
    public static final String THINKING_TEXT = "Let me think about this carefully.";

    public ThinkingChatModelMock() {}

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        return ChatResponse.builder()
                .aiMessage(
                        AiMessage.builder().text("ok").thinking(THINKING_TEXT).build())
                .tokenUsage(new TokenUsage(200))
                .build();
    }

    public static ThinkingChatModelMockBuilder builder() {
        return new ThinkingChatModelMockBuilder();
    }

    /** Required by {@link dev.langchain4j.cdi.plugin.CommonLLMPluginCreator} for plugin-based instantiation. */
    public static class ThinkingChatModelMockBuilder {

        public ThinkingChatModelMockBuilder() {}

        public ThinkingChatModelMock build() {
            return new ThinkingChatModelMock();
        }
    }
}
