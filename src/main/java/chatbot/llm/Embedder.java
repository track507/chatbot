package chatbot.llm;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import chatbot.helpers.HttpClientPool;

public class Embedder {
    // endpoint for Ollama's embedding api (local)
    private static final String DEFAULT_OLLAMA_EMBED_ENDPOINT = "http://localhost:11434/api/embeddings";
    // using nomic-embed-text model for text embedding (local)
    private static final String DEFAULT_MODEL_NAME = "nomic-embed-text";
    // my end point for qdrant (local)
    private static final String DEFAULT_QDRANT_ENDPOINT = "http://localhost:6333";

    // user defined
    private final String ollamaEmbedEndpoint;
    private final String modelName;
    private final String qdrantEndpoint;
    private final String collectionName;

    private final HttpClient httpClient = HttpClientPool.SHARED_CLIENT;
    private final ObjectMapper mapper = new ObjectMapper();

    public Embedder(String ollamaEmbedEndpoint, String modelName, String qdrantEndpoint) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> config = mapper.readValue(new File("options.json"), Map.class);
            this.collectionName = config.getOrDefault("qdrantCollection", "chatbot");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read options.json for collection name: " + e.getMessage(), e);
        }

        this.ollamaEmbedEndpoint = (ollamaEmbedEndpoint == null || ollamaEmbedEndpoint.isBlank())
            ? DEFAULT_OLLAMA_EMBED_ENDPOINT : ollamaEmbedEndpoint;

        this.modelName = (modelName == null || modelName.isBlank())
            ? DEFAULT_MODEL_NAME : modelName;

        this.qdrantEndpoint = (qdrantEndpoint == null || qdrantEndpoint.isBlank())
            ? DEFAULT_QDRANT_ENDPOINT : qdrantEndpoint;
    }

    public Embedder() {
        this(null, null, null);
    }    

    // check if the api endpoint is available
    public boolean isQdrantAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(qdrantEndpoint + "/collections"))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOllamaAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEmbedEndpoint.replace("/api/embeddings", "/")))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isModelAvailable() {
        try {
            String requestBody = mapper.writeValueAsString(Map.of(
                "model", modelName,
                "prompt", "test"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEmbedEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && mapper.readTree(response.body()).get("embedding") != null;
        } catch (Exception e) {
            return false;
        }
    }

    // call embedding api to do the embedding for us.
    public List<Float> getEmbedding(String content) throws EmbeddingException {
        try {
            // Use LinkedHashMap to preserve key order in the JSON
            Map<String, Object> requestMap = new LinkedHashMap<>();
            requestMap.put("model", modelName);
            requestMap.put("prompt", content);
    
            String requestBody = mapper.writeValueAsString(requestMap);
    
            //System.out.println("\nEmbedding request JSON:\n" + requestBody);  // Debug
    
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaEmbedEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    
            //System.out.println("\nOllama response:\n" + response.body());  // Debug
            JsonNode json = mapper.readTree(response.body());
            JsonNode embeddingArray = json.get("embedding");
    
            if (embeddingArray == null || !embeddingArray.isArray() || embeddingArray.size() == 0) {
                throw new EmbeddingException("Invalid response from Ollama (missing embedding array): " + response.body());
            }
    
            List<Float> embedding = new ArrayList<>();
            embeddingArray.forEach(e -> embedding.add(e.floatValue()));
    
            return embedding;
    
        } catch (IOException | InterruptedException e) {
            throw new EmbeddingException("Failed to get embedding from Ollama (" + modelName + "): " + e.getMessage(), e);
        }
    }

    public void chunkEmbeds(String filePath) throws EmbeddingException {
        try {
            Path path = Paths.get(filePath);
    
            if (!Files.exists(path)) {
                throw new EmbeddingException("File not found: " + filePath);
            }
    
            String content = Files.readString(path).trim();
    
            if (content.isEmpty()) {
                throw new EmbeddingException(filePath + " is empty or blank.");
            }
    
            final int chunkSize = 1000;
            final int overlap = 200;
    
            int chunkIndex = 0;
            for (int start = 0; start < content.length(); start += (chunkSize - overlap)) {
                int end = Math.min(content.length(), start + chunkSize);
                String chunk = content.substring(start, end);
    
                if (!chunk.isBlank()) {
                    List<Float> embedding = getEmbedding(chunk);
    
                    // unique id
                    String id = filePath + "_chunk_" + chunkIndex;
    
                    // payload with metadata
                    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
                    payload.put("content", chunk);
                    payload.put("chunk_index", chunkIndex);
                    payload.put("source", filePath);
    
                    storeEmbeddingInQdrant(id, embedding, payload);
                    System.out.println("Stored chunk " + chunkIndex + " from file.");
    
                    chunkIndex++;
                }
            }
    
        } catch (IOException e) {
            throw new EmbeddingException("Failed reading or chunking file: " + e.getMessage(), e);
        }
    }    

    // gather the embeddings and store them in Qdrant
    public void storeEmbeddingInQdrant(String id, List<Float> embedding, Map<String, Object> payload) throws EmbeddingException {
        try {
            if (id == null || id.isEmpty() || embedding == null || embedding.isEmpty()) {
                throw new IllegalArgumentException("Invalid parameters for embedding storage.");
            }
    
            String collectionName = this.collectionName;
            int vectorSize = embedding.size(); // get size from actual vector
    
            // ensure collection exists first
            ensureCollectionExists(collectionName, vectorSize);
    
            // prepare payload
            LinkedHashMap<String, Object> point = new LinkedHashMap<>();
            point.put("id", Math.abs(id.hashCode()));
            point.put("vector", embedding);

            // LinkedHashMap to ensure order
            point.put("payload", payload);

            // wrap the points
            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            root.put("points", List.of(point));

            String jsonPayload = mapper.writeValueAsString(root);
            //System.out.println("\nQdrant request JSON:\n" + jsonPayload);  // Debug
    
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(qdrantEndpoint + "/collections/" + collectionName + "/points"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    
            if (response.statusCode() != 200) {
                throw new EmbeddingException("Failed to store embedding in Qdrant: " + response.body());
            }
    
        } catch (IOException | InterruptedException e) {
            throw new EmbeddingException("Qdrant embedding storage error: " + e.getMessage(), e);
        }
    }

    public void deleteCollection(String collectionName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(qdrantEndpoint + "/collections/" + collectionName))
            .DELETE()
            .build();
    
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    
        if (response.statusCode() == 200) {
            System.out.println("Deleted existing Qdrant collection: " + collectionName);
        } else {
            System.out.println("Warning: Could not delete Qdrant collection '" + collectionName + "'. Response: " + response.body());
        }
    }    

    private void ensureCollectionExists(String collectionName, int vectorSize) throws IOException, InterruptedException {
        // check if collection exists
        HttpRequest listRequest = HttpRequest.newBuilder()
            .uri(URI.create(qdrantEndpoint + "/collections"))
            .GET()
            .build();
    
        HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
    
        JsonNode root = mapper.readTree(listResponse.body());
        boolean exists = root.path("result").path("collections")
            .findValuesAsText("name").contains(collectionName);
    
        if (!exists) {
            System.out.println("Creating Qdrant collection: " + collectionName);
    
            String json = mapper.writeValueAsString(Map.of(
                "vectors", Map.of(
                    "size", vectorSize,
                    "distance", "Cosine"
                )
            ));
    
            HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(qdrantEndpoint + "/collections/" + collectionName))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
    
            HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
    
            if (createResponse.statusCode() != 200) {
                throw new IOException("Failed to create collection: " + createResponse.body());
            }
    
            System.out.println("Collection '" + collectionName + "' created.");
        }
    }

    // an option, but not required. should be done anyways if rerunning the .sql scripts
    public void revectorizeAll() throws EmbeddingException {
        if (!isOllamaAvailable()) {
            throw new EmbeddingException("Ollama embedding endpoint is not reachable. Ensure Ollama is running.");
        }
        if (!isQdrantAvailable()) {
            throw new EmbeddingException("Qdrant is not reachable. Check the Tailscale network or Qdrant service.");
        }
        if (!isModelAvailable()) {
            throw new EmbeddingException("Embedding model '" + modelName + "' is unavailable. Check Ollama.");
        }

        try {
            deleteCollection(collectionName);
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to delete collection: " + e.getMessage());
        }

        System.out.println("Vectorizing database_export.txt...");
        chunkEmbeds("database_export.txt");
        System.out.println("Finished embedding: database_export.txt");
    }

    public void vectorizeUserInfo(String filePath) throws EmbeddingException {
        try {
            Path path = Paths.get(filePath);
    
            if (!Files.exists(path)) {
                throw new EmbeddingException("File not found: " + filePath);
            }
    
            String content = Files.readString(path).trim();
    
            if (content.isEmpty()) {
                throw new EmbeddingException("user_info.json is empty or blank.");
            }
    
            List<Float> embedding = getEmbedding(content);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", content);
            payload.put("source", "user_info.json");

            storeEmbeddingInQdrant("user_info.json", embedding, payload);
            System.out.println("Vectorized: user_info.json");
        } catch (IOException e) {
            throw new EmbeddingException("Failed to read user_info.json: " + e.getMessage(), e);
        }
    }

    public void setup() throws EmbeddingException {
        Embedder embedder = new Embedder(); // uses default endpoints
        embedder.revectorizeAll();
    }    
}
