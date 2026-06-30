package dev.langchain4j.cdi.example.booking;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Document RAG ingestor for loading car rental terms into the embedding store. */
@ApplicationScoped
public class DocRagIngestor {

    private static final Logger LOGGER = Logger.getLogger(DocRagIngestor.class.getName());

    // Used by ContentRetriever
    @Produces
    private EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    // Used by ContentRetriever
    @Produces
    private InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    @Inject
    @ConfigProperty(name = "app.docs-for-rag.dir")
    private File docs;

    /** Creates a new document RAG ingestor. */
    public DocRagIngestor() {}

    private List<Document> loadDocs() {
        return loadDocuments(docs.getPath(), new TextDocumentParser());
    }

    /**
     * Ingests car rental terms document into the embedding store.
     *
     * @param pointless the CDI event object (unused)
     */
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

    /**
     * Runs the document ingestion as a standalone process.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {

        System.out.println(InMemoryEmbeddingStore.class.getInterfaces()[0]);
    }
}
