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
// langchain4j's version of embedding search and results: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/store/embedding
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
// java stuff
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        List<TextSegment> nearestSegments = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(5)
                .build()).matches().stream()
                .map(match -> match.embedded())
                .collect(Collectors.toList());

        String context = nearestSegments.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n\n"));

        String prompt = "User Profile: " + userProfile + "\n\n" +
                "Relevant Context: " + context + "\n\n" +
                "User Question: " + userInput;

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

    public List<ChatMessage> getHistory() {
        return history;
    }
}
