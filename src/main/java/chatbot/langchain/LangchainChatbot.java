package chatbot.langchain;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.chain.StandardRetrievalAugmentedGenerationChain;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LangchainRAGChatbot {

    private final StandardRetrievalAugmentedGenerationChain ragChain;
    private static final String DEFAULT_OLLAMA_ENDPOINT = "http://localhost:11434";
    private static final String DEFAULT_QDRANT_ENDPOINT = "http://localhost:6333";
    private static final String DEFAULT_MODEL_NAME = "Advisor";
    private static final String DEFAULT_COLLECTION_NAME = "chatbot";

    public LangchainRAGChatbot() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> config = new LinkedHashMap<>();

        try {
            config = mapper.readValue(new File("options.json"), Map.class);
        } catch (IOException e) {
            System.err.println("Warning: Could not read options.json, using defaults. " + e.getMessage());
        }

        String ollamaEndpoint = config.getOrDefault("ollamaEndpoint", DEFAULT_OLLAMA_ENDPOINT);
        String modelName = config.getOrDefault("LLMModel", DEFAULT_MODEL_NAME);
        String qdrantUrl = config.getOrDefault("qdrantEndpoint", DEFAULT_QDRANT_ENDPOINT);
        String collection = config.getOrDefault("qdrantCollection", DEFAULT_COLLECTION_NAME);

        String qdrantHost = "localhost";
        int qdrantPort = 6333;

        try {
            URI qdrantUri = new URI(qdrantUrl);
            if (qdrantUri.getHost() != null) qdrantHost = qdrantUri.getHost();
            if (qdrantUri.getPort() != -1) qdrantPort = qdrantUri.getPort();
        } catch (URISyntaxException e) {
            System.err.println("Invalid Qdrant URL: " + qdrantUrl);
        }

        var model = OllamaLanguageModel.builder()
            .baseUrl(ollamaEndpoint)
            .modelName(modelName)
            .build();

        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
            .host(qdrantHost)
            .port(qdrantPort)
            .collectionName(collection)
            .build();

        var retriever = EmbeddingStoreRetriever.builder()
            .embeddingStore(embeddingStore)
            .topK(5)
            .build();

        this.ragChain = StandardRetrievalAugmentedGenerationChain.builder()
            .retriever(retriever)
            .languageModel(model)
            .build();
    }

    public String ask(String input) {
        return ragChain.execute(input);
    }
}
