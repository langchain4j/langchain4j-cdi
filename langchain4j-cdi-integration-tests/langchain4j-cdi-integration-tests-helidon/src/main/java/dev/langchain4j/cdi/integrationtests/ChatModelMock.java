package dev.langchain4j.cdi.integrationtests;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.enterprise.context.ApplicationScoped;

/** Mock {@link ChatModel} for Helidon integration testing. */
@ApplicationScoped
public class ChatModelMock implements ChatModel {

    /** Creates a new instance. */
    public ChatModelMock() {}

    private ChatResponse fixedChatResponse;

    /**
     * Sets a fixed response to return for all chat requests.
     *
     * @param fixedChatResponse the response to return
     */
    public void setFixedChatResponse(ChatResponse fixedChatResponse) {
        this.fixedChatResponse = fixedChatResponse;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        if (fixedChatResponse != null) {
            return fixedChatResponse;
        }
        return ChatResponse.builder()
                .aiMessage(new AiMessage("ok"))
                .tokenUsage(new TokenUsage(200))
                .build();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static ChatModelMockBuilder builder() {
        return new ChatModelMockBuilder();
    }

    /** Builder for {@link ChatModelMock}. */
    public static class ChatModelMockBuilder {

        /** Creates a new instance. */
        public ChatModelMockBuilder() {}

        /**
         * Builds the mock.
         *
         * @return a new {@link ChatModelMock}
         */
        public ChatModelMock build() {
            return new ChatModelMock();
        }
    }
}
