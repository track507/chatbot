package chatbot.langchain;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

import chatbot.helpers.SetupOptions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LangchainEmbedder {
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final String collectionName;
    private final String qdrantHost;
    private final int qdrantPort;

    public LangchainEmbedder() {
        SetupOptions options = SetupOptions.fromJson(Paths.get("options.json"));

        String ollamaEmbedEndpoint = options.getOllamaEmbedEndpoint();
        String modelName = options.getEmbedModel();
        String qdrantUrl = options.getQdrantEndpoint();
        this.collectionName = options.getQdrantCollection();

        String host = "localhost";
        int port = 6333;

        try {
            URI qdrantUri = new URI(qdrantUrl);
            if (qdrantUri.getHost() != null) host = qdrantUri.getHost();
            if (qdrantUri.getPort() != -1) port = qdrantUri.getPort();
        } catch (URISyntaxException e) {
            System.err.println("Invalid Qdrant URL: " + qdrantUrl);
        }

        this.qdrantHost = host;
        this.qdrantPort = port;

        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(ollamaEmbedEndpoint)
                .modelName(modelName)
                .build();

        this.embeddingStore = QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .collectionName(collectionName)
                .build();
    }

    public void embedFile(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath)).trim();
        if (content.isEmpty()) {
            System.err.println("Warning: File is empty: " + filePath);
            return;
        }

        List<String> chunks = chunkText(content, 1000, 200);
        for (String chunk : chunks) {
            TextSegment segment = TextSegment.from(chunk);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
        System.out.println("Embedded and stored " + chunks.size() + " segments from: " + filePath);
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = end - overlap;
            if (start < 0) start = 0;
        }
        return chunks;
    }

    public void embedUserProfile(String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                System.err.println("Warning: File not found: " + filePath);
                return;
            }

            String content = Files.readString(path).trim();
            if (content.isEmpty()) {
                System.err.println("Warning: user_info.json is empty or blank.");
                return;
            }

            TextSegment segment = TextSegment.from(content);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
            System.out.println("Embedded user_info.json");
        } catch (Exception e) {
            System.err.println("Failed to embed user_info.json: " + e.getMessage());
        }
    }

    public void revectorizeAll() throws IOException {
        deleteCollection();
        System.out.println("Deleted collection: " + collectionName);

        embedFile("database_export.txt");
    }

    public void deleteCollection() {
        try {
            String deleteUrl = "http://" + qdrantHost + ":" + qdrantPort + "/collections/" + collectionName;
            java.net.http.HttpClient.newHttpClient()
                .send(java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .DELETE()
                    .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            System.err.println("Failed to delete collection: " + e.getMessage());
        }
    }

    public void setup() throws EmbeddingException {
        try {
            revectorizeAll();
        } catch (IOException e) {
            throw new EmbeddingException("Failed during setup: " + e.getMessage(), e);
        }
    }
}