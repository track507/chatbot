package chatbot.langchain;

public class EmbeddingException extends Exception {
    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingException(String message) {
        super(message);
    }
}
