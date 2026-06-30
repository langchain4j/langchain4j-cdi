package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;

/** Mock {@link ChatModel} for Jakarta EE integration testing. */
@ApplicationScoped
public class ChatModelMock implements ChatModel {

    private final ChatResponse fixedChatResponse;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Creates a new instance.
     *
     * @param fixedChatResponse the fixed response to return, or {@code null}
     * @param embeddingStore the embedding store for response composition
     */
    public ChatModelMock(ChatResponse fixedChatResponse, EmbeddingStore<TextSegment> embeddingStore) {
        this.fixedChatResponse = fixedChatResponse;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        if (fixedChatResponse != null) {
            return fixedChatResponse;
        }
        return ChatResponse.builder()
                .aiMessage(new AiMessage("ok " + embeddingStore.toString()))
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

        private ChatResponse fixedChatResponse;
        private EmbeddingStore<TextSegment> embeddingStore;

        /**
         * Sets the fixed chat response.
         *
         * @param fixedChatResponse the response to return
         * @return this builder
         */
        public ChatModelMockBuilder fixedChatResponse(ChatResponse fixedChatResponse) {
            this.fixedChatResponse = fixedChatResponse;
            return this;
        }

        /**
         * Sets the embedding store.
         *
         * @param embeddingStore the embedding store
         * @return this builder
         */
        public ChatModelMockBuilder embeddingStore(EmbeddingStore<TextSegment> embeddingStore) {
            this.embeddingStore = embeddingStore;
            return this;
        }

        /**
         * Builds the mock.
         *
         * @return a new {@link ChatModelMock}
         */
        public ChatModelMock build() {
            return new ChatModelMock(fixedChatResponse, embeddingStore);
        }
    }
}
