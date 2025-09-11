package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

@ApplicationScoped
public class ChatModelMock implements ChatModel {

    private final ChatResponse fixedChatResponse;
    private final EmbeddingStore<TextSegment> embeddingStore;

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

    public static ChatModelMockBuilder builder() {
        return new ChatModelMockBuilder();
    }

    public static class ChatModelMockBuilder {

        private ChatResponse fixedChatResponse;
        private EmbeddingStore<TextSegment> embeddingStore;

        public ChatModelMockBuilder fixedChatResponse(ChatResponse fixedChatResponse) {
            this.fixedChatResponse = fixedChatResponse;
            return this;
        }

        public ChatModelMockBuilder embeddingStore(EmbeddingStore<TextSegment> embeddingStore) {
            this.embeddingStore = embeddingStore;
            return this;
        }

        public ChatModelMock build() {
            return new ChatModelMock(fixedChatResponse, embeddingStore);
        }

    }
}
