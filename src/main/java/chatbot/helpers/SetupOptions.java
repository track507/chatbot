package chatbot.helpers;

public class SetupOptions {
    public String embedModel;
    public String LLMModel;
    public String ollamaEndpoint;
    public String qdrantEndpoint;
    public String qdrantCollection;
    public String ollamaEmbedEndpoint;
    public String database;

    public SetupOptions() {};

    public SetupOptions(String embedModel, String LLMModel, String ollamaEndpoint, String ollamaEmbedEndpoint, String qdrantEndpoint, String qdrantCollection, String database) {
        this.embedModel = (embedModel == null || embedModel.isBlank()) ? "nomic-embed-text" : embedModel;
        this.LLMModel = (LLMModel == null || LLMModel.isBlank()) ? "gemma3:1b" : LLMModel;
        this.ollamaEndpoint = (ollamaEndpoint == null || ollamaEndpoint.isBlank())
                ? "http://localhost:11434" : ollamaEndpoint;
        this.ollamaEmbedEndpoint = (ollamaEmbedEndpoint == null || ollamaEmbedEndpoint.isBlank())
                ? "http://localhost:11434/api/embeddings" : ollamaEmbedEndpoint;
        this.qdrantEndpoint = (qdrantEndpoint == null || qdrantEndpoint.isBlank())
                ? "http://localhost:6333" : qdrantEndpoint;
        this.qdrantCollection = (qdrantCollection == null || qdrantCollection.isBlank())
                ? "chatbot" : qdrantCollection;
        this.database = (database == null || database.isBlank())
                ? "main.db" : database;
    }
}
