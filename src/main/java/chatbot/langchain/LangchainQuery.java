package chatbot.langchain;
import chatbot.helpers.SetupOptions;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/message
import dev.langchain4j.data.message.*;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/embedding
import dev.langchain4j.data.embedding.Embedding;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/segment
import dev.langchain4j.data.segment.TextSegment;
// https://github.com/langchain4j/langchain4j/tree/77b08e6e12aa3b4c43f8d65a9ecc9df95d67711e/langchain4j-core/src/main/java/dev/langchain4j/model/chat
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-ollama/src/main/java/dev/langchain4j/model/ollama
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-qdrant/src/main/java/dev/langchain4j/store/embedding/qdrant
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/store/embedding/filter
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
// langchain4j's version of embedding search and results: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/store/embedding
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
// java stuff
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LangchainQuery {
    private static final Logger logger = Logger.getLogger(LangchainQuery.class.getName());

    private final StreamingChatModel model;
    private final EmbeddingModel embeddingModel;
    private final QdrantEmbeddingStore embeddingStore;
    private final List<ChatMessage> history = new ArrayList<>();

    public LangchainQuery(SetupOptions options) {
        this.model = OllamaStreamingChatModel.builder()
                .baseUrl(options.getOllamaEndpoint())
                .modelName(options.getChatModel())
                .build();
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(options.getOllamaEmbedEndpoint())
                .modelName(options.getEmbedModel())
                .build();
        this.embeddingStore = QdrantEmbeddingStore.builder()
                .collectionName(options.getQdrantCollection())
                .host(options.getQdrantGrpcHost())
                .port(options.getQdrantGrpcPort())
                .build();
    }

    public void streamQuery(String userInput, String userProfile, Consumer<String> onToken, Consumer<String> onComplete) {
        history.add(UserMessage.from(userInput));

        Embedding questionEmbedding = embeddingModel.embed(userInput).content();

        // extract all the known catalog diractory labels from data directory
        List<String> knownCatalogs = getKnownCatalogs();

        Map<String, String> yearToFolder = new HashMap<>();
        try {
            yearToFolder = buildYearToCatalogMap(Paths.get("data"));
        } catch (IOException e) {
            logger.warning("Failed to build year-to-folder map: " + e.getMessage());
        }
        Set<String> matchedCatalogs = yearToFolder.entrySet().stream()
            .filter(entry -> userInput.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        Filter filter = null;
        if (!matchedCatalogs.isEmpty()) {
            filter = MetadataFilterBuilder
                .metadataKey("catalog")
                .isIn(matchedCatalogs);
        }

        List<TextSegment> nearestSegments = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(10)
                .minScore(0.75)
                .filter(filter)
                .build()
        ).matches().stream()
        .map(EmbeddingMatch::embedded)
        .collect(Collectors.toList());


        Map<String, List<String>> catalogToText = new HashMap<>();
        for (TextSegment seg : nearestSegments) {
            String catalog = seg.metadata().getString("catalog");
            catalogToText.computeIfAbsent(catalog, k -> new ArrayList<>()).add(seg.text());
        }

        StringBuilder context = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : catalogToText.entrySet()) {
            context.append("=== Context from ").append(entry.getKey()).append(" ===\n");
            for (String chunk : entry.getValue()) {
                context.append(chunk).append("\n\n");
            }
        }

        String prompt = String.format("""
        You are an academic advisor. Use the context provided from one or more university catalogs to answer the user's question clearly and accurately.

        User Profile:
        %s

        Relevant Context:
        %s

        User's Question:
        %s

        Instructions:
        Use only the information provided in the context above. When multiple catalogs are involved, clearly label which catalog each fact or requirement comes from. Do not make assumptions.
        Only provide the catalog year and name if the user asks for it or if it's relevant to the answer. Do not include any other information about the catalog or university.
        Always use the latest catalog year unless it's included in the user profile or if the user specifies otherwise.
        """, userProfile, context, userInput);

        List<ChatMessage> augmentedMessages = new ArrayList<>(history);
        augmentedMessages.add(UserMessage.from(prompt));

        model.chat(augmentedMessages, new StreamingChatResponseHandler() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void onPartialResponse(String token) {
                buffer.append(token);
                onToken.accept(token);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                AiMessage assistantMessage = response.aiMessage();
                history.add(assistantMessage);
                onComplete.accept(assistantMessage.text());
            }

            @Override
            public void onError(Throwable error) {
                logger.severe("Streaming error: " + error.getMessage());
            }
        });
    }

    private List<String> extractCatalogLabels(String userInput, List<String> knownCatalogs) {
        return knownCatalogs.stream()
            .filter(userInput::contains)
            .collect(Collectors.toList());
    }

    public List<String> getKnownCatalogs() {
        try {
            return Files.list(Paths.get("data"))
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.warning("Failed to list catalog folders: " + e.getMessage());
            return List.of(); // fallback to empty list
        }
    }

    public static Map<String, String> buildYearToCatalogMap(Path dataDir) throws IOException {
        Map<String, String> map = new HashMap<>();

        List<Path> dirs = Files.list(dataDir)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());

        for (Path dir : dirs) {
            String folder = dir.getFileName().toString();

            // extract first 4-digit year from folder name
            Pattern pattern = Pattern.compile("(20\\d{2})[\\-–—/](20\\d{2})");
            Matcher matcher = pattern.matcher(folder);
            if (matcher.find()) {
                String startYear = matcher.group(1);
                String endYear = matcher.group(2);
                map.put(startYear, folder);
                map.put(endYear, folder);
            }
        }
        return map;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }
}
