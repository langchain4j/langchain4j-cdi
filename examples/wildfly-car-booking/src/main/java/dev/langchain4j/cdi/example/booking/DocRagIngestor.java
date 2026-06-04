package dev.langchain4j.cdi.example.booking;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DocRagIngestor {

    private static final Logger LOGGER = Logger.getLogger(DocRagIngestor.class.getName());

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    @ConfigProperty(name = "app.docs-for-rag.dir")
    private File docs;

    private List<Document> loadDocs() {
        return loadDocuments(docs.getPath(), new TextDocumentParser());
    }

    public void ingest(@Observes @Initialized(ApplicationScoped.class) Object pointless) {

        long start = System.currentTimeMillis();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 30))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        List<Document> docs = loadDocs();
        ingestor.ingest(docs);

        LOGGER.info(String.format(
                "DEMO %d documents ingested in %d msec", docs.size(), System.currentTimeMillis() - start));
    }
}
