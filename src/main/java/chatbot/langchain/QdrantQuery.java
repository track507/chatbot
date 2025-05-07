package chatbot.langchain;
import chatbot.helpers.SetupOptions;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
// langchain4j's specific mbedding search and results: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/store/embedding
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
// https://github.com/langchain4j/langchain4j/blob/main/langchain4j-qdrant/src/main/java/dev/langchain4j/store/embedding/qdrant/QdrantEmbeddingStore.java
// ! This uses grpc, not REST. Make sure to use the correct Qdrant endpoint. The REST API Will fail (trust me I tried)
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

public class QdrantQuery {
    private static final Logger logger = Logger.getLogger(QdrantQuery.class.getName());
    private final QdrantEmbeddingStore embeddingStore;

    public QdrantQuery(SetupOptions options) {
        String host = options.getQdrantGrpcHost();
        int port = options.getQdrantGrpcPort();
        try {
            URI uri = new URI(host);
            if (uri.getPort() != -1) {
                port = uri.getPort();
                host = uri.getHost();
            }
        } catch (URISyntaxException e) {
            logger.warning("Invalid Qdrant endpoint URI: " + e.getMessage());
        }

        this.embeddingStore = QdrantEmbeddingStore.builder()
                .collectionName(options.getQdrantCollection())
                .host(host)
                .port(port)
                .build();
    }

    public EmbeddingSearchResult<TextSegment> search(Embedding embedding, int k) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(k)
                .build();
        return embeddingStore.search(request);
    }
}
