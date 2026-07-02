package dev.langchain4j.cdi.core.integrationtests;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/** No-op {@link EmbeddingStore} of {@link String} for integration testing. */
@ApplicationScoped
public class EmbeddingStoreString implements EmbeddingStore<String> {

    /** Creates a new instance. */
    public EmbeddingStoreString() {}

    @Override
    public String add(Embedding embedding) {
        return "";
    }

    @Override
    public void add(String s, Embedding embedding) {}

    @Override
    public String add(Embedding embedding, String s) {
        return "";
    }

    @Override
    public List<String> addAll(List<Embedding> list) {
        return List.of();
    }

    @Override
    public EmbeddingSearchResult<String> search(EmbeddingSearchRequest embeddingSearchRequest) {
        return null;
    }

    @Override
    public String toString() {
        return "EmbeddingStoreString{}";
    }
}
