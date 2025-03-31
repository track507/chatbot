package chatbot.helpers;

public class SetupOptions {
    public String model;
    public String ollamaEndpoint;
    public String qdrantEndpoint;
    public String ollamaEmbedEndpoint;
    public String database;

    public SetupOptions() {};

    public SetupOptions(String model, String ollamaEndpoint, String ollamaEmbedEndpoint, String qdrantEndpoint, String database) {
        this.model = (model == null || model.isBlank()) ? "nomic-embed-text" : model;
        this.ollamaEndpoint = (ollamaEndpoint == null || ollamaEndpoint.isBlank())
                ? "http://localhost:11434" : ollamaEndpoint;
        this.ollamaEmbedEndpoint = (ollamaEmbedEndpoint == null || ollamaEmbedEndpoint.isBlank())
                ? "http://localhost:11434/api/embeddings" : ollamaEmbedEndpoint;
        this.qdrantEndpoint = (qdrantEndpoint == null || qdrantEndpoint.isBlank())
                ? "http://localhost:6333" : qdrantEndpoint;
        this.database = (database == null || database.isBlank())
                ? "main.db" : database;
    }
}
