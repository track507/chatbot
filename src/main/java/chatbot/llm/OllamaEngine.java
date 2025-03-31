package chatbot.llm;
import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class OllamaEngine {
    private final String modelName;
    private final String endpoint = "http://localhost:11434/generate";

    public OllamaEngine(String modelName) {
        this.modelName = modelName;
    }

    public String getResponse(String prompt) {
        try {
            URL url = URI.create(endpoint).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // prepare JSON request body
            JSONObject json = new JSONObject();
            json.put("model", modelName);
            json.put("prompt", prompt);
            json.put("stream", false);

            try(OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes());
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("response");

        } catch (Exception e) {
            System.out.println("Error processing input: " + e.getMessage());
            return null;
        }
    }
}