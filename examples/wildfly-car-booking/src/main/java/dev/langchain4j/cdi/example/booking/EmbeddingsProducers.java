package dev.langchain4j.cdi.example.booking;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/** CDI producer for embedding model and embedding store beans. */
@ApplicationScoped
public class EmbeddingsProducers {

    /** Creates a new embeddings producer. */
    public EmbeddingsProducers() {}

    /**
     * Produces the embedding model bean.
     *
     * @return the all-MiniLM-L6-v2 embedding model
     */
    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Produces the in-memory embedding store bean.
     *
     * @return a new in-memory embedding store
     */
    @Produces
    @ApplicationScoped
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
