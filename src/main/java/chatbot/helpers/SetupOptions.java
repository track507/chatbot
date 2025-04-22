package chatbot.helpers;

public class SetupOptions {
	public String embedModel;
	public String LLMModel;
	public String ollamaEndpoint;
	public String ollamaEmbedEndpoint;
	public String qdrantEndpoint;
	public String qdrantCollection;
	public String database;
	public String userInfoFile;
	public String databaseExportFile;

	public SetupOptions() {};

	public SetupOptions(String embedModel, String LLMModel, String ollamaEndpoint, String ollamaEmbedEndpoint, String qdrantEndpoint, String qdrantCollection, String database) {
		this.embedModel = (embedModel == null || embedModel.isBlank()) ? "nomic-embed-text" : embedModel;
		this.LLMModel = (LLMModel == null || LLMModel.isBlank()) ? "Advisor" : LLMModel;
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
	public String getEmbedModel() { return embedModel; }
	public String getLLMModel() { return LLMModel; }
	public String getOllamaEndpoint() { return ollamaEndpoint; }
	public String getOllamaEmbedEndpoint() { return ollamaEmbedEndpoint; }
	public String getQdrantEndpoint() { return qdrantEndpoint; }
	public String getQdrantCollection() { return qdrantCollection; }
	public String getDatabase() { return database; }

	public static SetupOptions fromJson(java.nio.file.Path path) {
			try {
					return new com.fasterxml.jackson.databind.ObjectMapper().readValue(path.toFile(), SetupOptions.class);
			} catch (Exception e) {
					throw new RuntimeException("Failed to load SetupOptions: " + e.getMessage(), e);
			}
	}
}
