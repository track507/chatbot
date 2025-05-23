/**
 * 
 * Implements streaming Langchain engine using /api/chat.
 * Streams LLM responses token-by-token with history.
 * 
 * https://github.com/langchain4j/langchain4j-examples/blob/main/ollama-examples/src/main/java/OllamaStreamingChatModelTest.java
 * 
 */

package chatbot.langchain;
import chatbot.helpers.SetupOptions;
// used for message history: https://github.com/langchain4j/langchain4j/tree/main/langchain4j-core/src/main/java/dev/langchain4j/data/message
import dev.langchain4j.data.message.*;
// model stuff for streaming: https://github.com/langchain4j/langchain4j/tree/77b08e6e12aa3b4c43f8d65a9ecc9df95d67711e/langchain4j-core/src/main/java/dev/langchain4j/model/chat
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
// https://github.com/langchain4j/langchain4j/tree/main/langchain4j-ollama/src/main/java/dev/langchain4j/model/ollama
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class LangchainEngine {

    private static final Logger logger = Logger.getLogger(LangchainEngine.class.getName());
    private final List<ChatMessage> messageHistory = new ArrayList<>();
    private final StreamingChatModel chatModel;

    public LangchainEngine(SetupOptions options) {
        this.chatModel = OllamaStreamingChatModel.builder()
                .baseUrl(options.getOllamaEndpoint())
                .modelName(options.getChatModel())
                .build();
    }

    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }
}