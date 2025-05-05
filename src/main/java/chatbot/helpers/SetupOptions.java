package chatbot.helpers;

public class SetupOptions {
	public String embedModel;
	public String chatModel;
	public String ollamaEndpoint;
	public String ollamaEmbedEndpoint;
	public String qdrantRestEndpoint;
	public String qdrantCollection;
	public String database;
	public String qdrantGrpcHost;
	public int qdrantGrpcPort;

	public SetupOptions() {};

	public SetupOptions(String embedModel, String chatModel, String ollamaEndpoint, String ollamaEmbedEndpoint, String qdrantRestEndpoint, String qdrantGrpcHost, String qdrantGrpcPort, String qdrantCollection, String database) {
		this.embedModel = (embedModel == null || embedModel.isBlank()) ? "nomic-embed-text" : embedModel;
		this.chatModel = (chatModel == null || chatModel.isBlank()) ? "Advisor" : chatModel;
		this.ollamaEndpoint = (ollamaEndpoint == null || ollamaEndpoint.isBlank())
				? "http://localhost:11434" : ollamaEndpoint;
		this.ollamaEmbedEndpoint = (ollamaEmbedEndpoint == null || ollamaEmbedEndpoint.isBlank())
				? "http://localhost:11434" : ollamaEmbedEndpoint;
		this.qdrantRestEndpoint = (qdrantRestEndpoint == null || qdrantRestEndpoint.isBlank())
				? "http://localhost:6333" : qdrantRestEndpoint;
		this.qdrantGrpcHost = (qdrantGrpcHost == null || qdrantGrpcHost.isBlank())
    			? "localhost" : qdrantGrpcHost;
		this.qdrantGrpcPort = (qdrantGrpcPort == null || qdrantGrpcPort.isBlank()) 
				? 6334 : Integer.parseInt(qdrantGrpcPort);
		this.qdrantCollection = (qdrantCollection == null || qdrantCollection.isBlank())
				? "chatbot" : qdrantCollection;
		this.database = (database == null || database.isBlank())
				? "main.db" : database;
	}

	// getters, this made my life a lot easier
	public String getEmbedModel() { return embedModel; }
	public String getChatModel() { return chatModel; }
	public String getOllamaEndpoint() { return ollamaEndpoint; }
	public String getOllamaEmbedEndpoint() { return ollamaEmbedEndpoint; }
	public String getQdrantRestEndpoint() { return qdrantRestEndpoint; }
	public String getQdrantGrpcHost() { return qdrantGrpcHost; }
	public int getQdrantGrpcPort() { return qdrantGrpcPort; } // mandatory to use with ollamaclient: https://github.com/langchain4j/langchain4j/blob/main/langchain4j-ollama/src/main/java/dev/langchain4j/model/ollama/OllamaClient.java
	public String getQdrantCollection() { return qdrantCollection; }
	public String getDatabase() { return database; }

	public static SetupOptions fromJson(java.nio.file.Path path) {
		try {
				return new com.fasterxml.jackson.databind.ObjectMapper().readValue(path.toFile(), SetupOptions.class);
		} catch (Exception e) {
				throw new RuntimeException("Failed to load SetupOptions: " + e.getMessage(), e);
		}
	}

	public static SetupOptions load() {
		return fromJson(java.nio.file.Paths.get("options.json"));
	}
}
