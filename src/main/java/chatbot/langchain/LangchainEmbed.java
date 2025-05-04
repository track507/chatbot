/**
 * 
 * ! This is used to embed using Ollama API /api/embed and store in Qdrant.
 * /api/embed is the new API used, /api/embeddings is deprecated.
 * 
 * Used for storing/retrieving embeddings in Qdrant.
 * https://github.com/langchain4j/langchain4j-examples/blob/main/qdrant-example/src/main/java/QdrantEmbeddingStoreExample.java
 * 
 * Use this ollama API for embeddings:
 * https://github.com/langchain4j/langchain4j-examples/blob/main/ollama-examples/src/main/java/OllamaStreamingChatModelTest.java
 * 
 */

package chatbot.langchain;
import chatbot.helpers.SetupOptions;
// document stuff: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/document
import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;
import dev.langchain4j.data.document.*;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.source.FileSystemSource;
// used for embedding: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/embedding
import dev.langchain4j.data.embedding.Embedding;
// langchains4j implementation of textsegments: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/segment
import dev.langchain4j.data.segment.TextSegment;
// things for ollama embeds: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/model/embedding
import dev.langchain4j.model.embedding.EmbeddingModel;
// ollama specific embedding model: https://github.com/langchain4j/langchain4j/blob/main/langchain4j-ollama/src/main/java/dev/langchain4j/model/ollama/OllamaEmbeddingModel.java
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
// generic embedding store: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/store/embedding
import dev.langchain4j.store.embedding.EmbeddingStore;
// qdrant specific embedding store: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-qdrant/src/main/java/dev/langchain4j/store/embedding/qdrant
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
// mapper stuff
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
// trying out okthttp3 for http pooling
import okhttp3.OkHttpClient;
import okhttp3.ConnectionPool;
// all java imports not from pacakages
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class LangchainEmbed {
    private final SetupOptions options;
    private static final Logger logger = Logger.getLogger(LangchainEmbed.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter splitter;
    private final OkHttpClient httpClient;

    public LangchainEmbed(SetupOptions options) {
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(options.getOllamaEmbedEndpoint())
                .modelName(options.getEmbedModel())
                .build();
        
        logger.info("Using embedding model: " + options.getEmbedModel());
        logger.info("Using Ollama endpoint: " + options.getOllamaEmbedEndpoint());
        this.options = options;
        this.splitter = recursive(1000, 200); // same chunk size as in the original code (1000 chearacters, 200 overlap)
        ConnectionPool connectionPool = new ConnectionPool(
            10,
            5, TimeUnit.MINUTES
        );

        this.httpClient = new OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .build();
    }

    // check if the api endpoint is available
    public boolean isQdrantAvailable() {
        try {
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(options.getQdrantRestEndpoint() + "/collections")
                .get()
                .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOllamaAvailable() {
        try {
            String baseUrl = options.getOllamaEmbedEndpoint().replace("/api/embed", "/");

            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(baseUrl)
                .get()
                .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isModelAvailable() {
        try {
            String json = mapper.writeValueAsString(Map.of(
                "model", options.getEmbedModel(),
                "input", "test"
            ));

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                json,
                okhttp3.MediaType.parse("application/json")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(options.getOllamaEmbedEndpoint())
                .post(body)
                .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return false;

                String responseBody = response.body().string();
                JsonNode root = mapper.readTree(responseBody);

                // debug
                logger.info("Response from Ollama: " + responseBody);
                return (root.path("embedding").isArray() && root.path("embedding").size() > 0) || (root.path("embeddings").isArray() && root.path("embeddings").size() > 0);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteCollection(String collectionName) throws IOException {
        String url = options.getQdrantRestEndpoint() + "/collections/" + collectionName;

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(url)
            .delete()
            .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to delete collection: " + response.body().string());
            }
        }
    }

    public List<Embedding> chunkEmbeds(Path filePath) {
        logger.info("Loading document from file: " + filePath);

        Document document = DocumentLoader.load(
            new FileSystemSource(filePath),
            new TextDocumentParser()
        );

        logger.info("Document loaded. Beginning chunking...");
        List<TextSegment> chunks = splitter.split(document);
        logger.info("Chunking complete. Total chunks: " + chunks.size());

        logger.info("Generating embeddings...");
        List<Embedding> embeddings = chunks.stream()
            .map(chunk -> {
                logger.info("Embedding chunk: \"" + truncate(chunk.text(), 100) + "\"");
                return embeddingModel.embed(chunk.text()).content();
            })
            .collect(Collectors.toList());

        logger.info("Embedding generation complete. Total embeddings: " + embeddings.size());
        return embeddings;
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private void ensureCollectionExists(String collectionName, int vectorSize) throws IOException {
        okhttp3.Request listRequest = new okhttp3.Request.Builder()
            .url(options.getQdrantRestEndpoint() + "/collections")
            .get()
            .build();

        try (okhttp3.Response listResponse = httpClient.newCall(listRequest).execute()) {
            if (!listResponse.isSuccessful()) {
                throw new IOException("Failed to list collections: " + listResponse.body().string());
            }

            JsonNode root = mapper.readTree(listResponse.body().string());
            boolean exists = root.path("result").path("collections")
                .findValuesAsText("name").contains(collectionName);

            if (exists) {
                logger.info("Qdrant collection '" + collectionName + "' already exists.");
                return;
            }

            logger.info("Creating Qdrant collection: " + collectionName);

            String json = mapper.writeValueAsString(Map.of(
                "vectors", Map.of(
                    "size", vectorSize,
                    "distance", "Cosine"
                )
            ));

            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                json,
                okhttp3.MediaType.parse("application/json")
            );

            okhttp3.Request createRequest = new okhttp3.Request.Builder()
                .url(options.getQdrantRestEndpoint() + "/collections/" + collectionName)
                .put(requestBody)
                .build();

            try (okhttp3.Response createResponse = httpClient.newCall(createRequest).execute()) {
                if (!createResponse.isSuccessful()) {
                    throw new IOException("Failed to create collection: " + createResponse.body().string());
                }

                logger.info("Qdrant collection '" + collectionName + "' created successfully.");
            }

        } catch (IOException e) {
            logger.severe("Error ensuring Qdrant collection exists: " + e.getMessage());
            throw e;
        }
    }
    public void storeEmbeddingsInQdrant(Path filePath, SetupOptions options) throws EmbeddingException, IOException {
        Document document = DocumentLoader.load(
            new FileSystemSource(filePath),
            new TextDocumentParser()
        );
        List<TextSegment> chunks = splitter.split(document);

        List<TextSegment> segments = new ArrayList<>();
        List<Embedding> embeddings = new ArrayList<>();

        logger.info("Starting embedding for " + chunks.size() + " chunks...");

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i).text();
            String chunkId = "chunk-" + i;

            Metadata metadata = Metadata.from(Map.of("id", chunkId));
            segments.add(TextSegment.from(chunkText, metadata));
            embeddings.add(embeddingModel.embed(chunkText).content());

            if (i % 10 == 0 || i == chunks.size() - 1) {
                logger.info("Embedded chunk " + (i + 1) + "/" + chunks.size());
            }
        }

        ensureCollectionExists(options.getQdrantCollection(), embeddings.get(0).vector().length);

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
            return;
        }

        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                .collectionName(options.getQdrantCollection())
                .host(host)
                .port(port)
                .build();

        embeddingStore.addAll(embeddings, segments);
        logger.info("Stored " + embeddings.size() + " embeddings into Qdrant.");
    }

    public void vectorizeUserInfo(String filePath, SetupOptions options) throws EmbeddingException, IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new EmbeddingException("File not found: " + filePath);
        }

        try {
            String content = Files.readString(path).trim();

            if (content.isEmpty()) {
                throw new EmbeddingException(filePath + " is empty or blank.");
            }

            // Create embedding from user profile content
            Embedding embedding = embeddingModel.embed(content).content();

            // Create payload with metadata
            TextSegment segment = TextSegment.from(
                content,
                Metadata.from(Map.of("id", "user_info"))
            );

            // Setup embedding store
            String host = options.getQdrantGrpcHost();
            int port = options.getQdrantGrpcPort();
            try {
                ensureCollectionExists(options.getQdrantCollection(), embedding.vector().length);
                URI uri = new URI(host);
                if (uri.getPort() != -1) {
                    port = uri.getPort();
                    host = uri.getHost();
                }
            } catch (URISyntaxException e) {
                logger.warning("Invalid Qdrant endpoint URI: " + e.getMessage());
                throw new EmbeddingException("Invalid Qdrant URI", e);
            }

            EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                    .collectionName(options.getQdrantCollection())
                    .host(host)
                    .port(port)
                    .build();

            // Store embedding in Qdrant
            embeddingStore.add(embedding, segment);

            logger.info("Successfully vectorized and stored: " + filePath);

        } catch (IOException e) {
            throw new EmbeddingException("Failed to read " + filePath + ": " + e.getMessage(), e);
        }
    }

    public void revectorizeAll() {
        try {
            if (!isOllamaAvailable()) {
                throw new EmbeddingException("Ollama embedding endpoint is not reachable. Ensure Ollama is running.");
            }
            if (!isQdrantAvailable()) {
                throw new EmbeddingException("Qdrant is not reachable. Check the Tailscale network or Qdrant service.");
            }
            if (!isModelAvailable()) {
                throw new EmbeddingException("Embedding model '" + options.getEmbedModel() + "' is unavailable. Check Ollama.");
            }
        } catch (EmbeddingException e) {
            logger.warning("Revectorization failed during availability checks: " + e.getMessage());
            return;
        }

        try {
            deleteCollection(options.getQdrantCollection());
        } catch (IOException e) {
            System.err.println("Failed to delete collection: " + e.getMessage());
        }

        logger.info("Re-vectorizing all chunks...");
        try {
            storeEmbeddingsInQdrant(Paths.get("database_export.txt"), options);
        } catch (Exception e) {
            logger.severe("Failed to store embeddings during revectorization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
