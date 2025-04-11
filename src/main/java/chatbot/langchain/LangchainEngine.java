package chatbot.langchain;

import chatbot.helpers.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AssistantMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;


public class LangchainEngine {
    private final EmbeddingStoreRetriever retriever;
    private final ChatLanguageModel chatLlm;
    private final String userProfile;
    private final List<ChatMessage> messageHistory = new ArrayList<>();
    private final static String DEFAULT_PROMPT_TEMPLATE = """
        [USER PROFILE]
        {{ user_profile }}

        [CONTEXT]
        {{ context }}

        Question: {{ question }}
        """;

    public LangchainEngine() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> config = new LinkedHashMap<>();

        try {
            config = mapper.readValue(new File("options.json"), Map.class);
        } catch (IOException e) {
            System.err.println("Warning: Could not read options.json, using defaults. " + e.getMessage());
        }

        String ollamaEndpoint = config.getOrDefault("ollamaEndpoint", "http://localhost:11434");
        String embedModelName = config.getOrDefault("embedModel", "nomic-embed-text");
        String llmModelName = config.getOrDefault("LLMModel", "Advisor");
        String qdrantUrl = config.getOrDefault("qdrantEndpoint", "http://localhost:6333");
        String qdrantCollection = config.getOrDefault("qdrantCollection", "chatbot");

        String qdrantHost = "localhost";
        int qdrantPort = 6333;

        try {
            URI qdrantUri = new URI(qdrantUrl);
            if (qdrantUri.getHost() != null) qdrantHost = qdrantUri.getHost();
            if (qdrantUri.getPort() != -1) qdrantPort = qdrantUri.getPort();
        } catch (URISyntaxException e) {
            System.err.println("Invalid Qdrant URL: " + qdrantUrl);
        }

        EmbeddingStore<TextSegment> store = QdrantEmbeddingStore.builder()
            .host(qdrantHost)
            .port(qdrantPort)
            .collectionName(qdrantCollection)
            .build();

        this.retriever = EmbeddingStoreRetriever.builder()
            .embeddingStore(store)
            .topK(5)
            .build();

        this.chatLlm = OllamaLanguageModel.builder()
            .baseUrl(ollamaEndpoint)
            .modelName(llmModelName)
            .build()
            .asChatLanguageModel();

        // Load and format user profile
        String profile = "";
        try {
            Map<String, Object> user = mapper.readValue(new File("user_info.json"), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            profile = new Utils().formatUserProfile(user);
        } catch (IOException e) {
            System.err.println("Warning: Could not read user_info.json: " + e.getMessage());
        }
        this.userProfile = profile;
        messageHistory.add(SystemMessage.from(userProfile));
    }

    public String ask(String question) {
        try {
            // Retrieve relevant context from Qdrant
            List<TextSegment> docs = retriever.findRelevant(TextSegment.from(question));
            String context = docs.stream().map(TextSegment::text).collect(Collectors.joining("\n"));

            // Inject the context as a System message on first turn
            if (messageHistory.stream().noneMatch(msg -> msg instanceof SystemMessage && msg.text().contains("[CONTEXT]"))) {
                String systemMsg = "[CONTEXT]\n" + context;
                messageHistory.add(SystemMessage.from(systemMsg));
            }

            // Add user's question
            messageHistory.add(UserMessage.from(question));

            // Generate reply
            String reply = chatLlm.generate(messageHistory);

            // Add assistant's reply
            messageHistory.add(AssistantMessage.from(reply));

            return reply;

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public void resetConversation() {
        messageHistory.clear();
        messageHistory.add(SystemMessage.from(userProfile));
        System.out.println("Chat history cleared.");
    }
}
