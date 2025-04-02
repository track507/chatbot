package chatbot.llm;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import chatbot.helpers.HttpClientPool;

public class OllamaEngine {
    private final String modelName;
    private final String endpoint;
    private final List<JSONObject> messageHistory = new ArrayList<>();
    private final HttpClient httpClient = HttpClientPool.SHARED_CLIENT;

    public OllamaEngine() {
        this.modelName = loadModelName();
        this.endpoint = loadChatEndpoint();
    
        String systemContent = """
        You are an academic advisor chatbot named Advisor. Your job is to assist students using both their user profile and university academic records (context).
        Always greet or refer to the student by their first name when relevant.

        Be concise, clear, and friendly in your tone. Provide helpful, accurate, and personalized responses.
        Avoid making assumptions about the user or their situation. If you are unsure, ask clarifying questions.
        Avoid repeating unnecessary details already clear from context.
        """;
    
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemContent);
        messageHistory.add(systemMsg);
    }

    private String loadModelName() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> config = mapper.readValue(new File("options.json"), Map.class);
            String modelName = config.getOrDefault("LLMModel", "gemma3:4b");
            return modelName;
        } catch (Exception e) {
            System.out.println("Error loading LLM model: " + e.getMessage());
            return "gemma3:4b"; // default LLM
        }
    }

    private String loadChatEndpoint() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> config = mapper.readValue(new File("options.json"), Map.class);
            String endpoint = config.getOrDefault("ollamaEndpoint", "http://localhost:11434");
            return endpoint.endsWith("/") ? endpoint : endpoint + "/api/chat";
        } catch (Exception e) {
            System.out.println("Error loading chat endpoint: " + e.getMessage());
            return "http://localhost:11434/api/chat"; // default endpoint
        }
    }

    public String getResponse(String userMessage) {
        try {
            // append user message
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messageHistory.add(userMsg);

            // json request stuff
            JSONObject json = new JSONObject();
            json.put("model", modelName);
            json.put("messages", new JSONArray(messageHistory));
            json.put("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            String assistantReply = jsonResponse.getJSONObject("message").getString("content");

            // add llm response to msg history
            JSONObject assistantMsg = new JSONObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", assistantReply);
            messageHistory.add(assistantMsg);

            return assistantReply;

        } catch (Exception e) {
            System.out.println("Error in chat response: " + e.getMessage());
            return null;
        }
    }

    // incase things go awry
    public void resetConversation() {
        messageHistory.clear();
        String systemContent = """
        You are an academic advisor chatbot. Use user profile and context to give helpful, personalized responses.
        Keep responses concise and relevant. Avoid unnecessary assumptions.
        """;
    
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemContent);
        messageHistory.add(systemMsg);
    }
}