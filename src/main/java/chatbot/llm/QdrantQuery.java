package chatbot.llm;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class QdrantQuery {
    private final String endpoint;
    private final String collection;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public QdrantQuery() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> config = mapper.readValue(new File("options.json"), Map.class);
        this.endpoint = config.getOrDefault("qdrantEndpoint", "http://localhost:6333");
        this.collection = config.getOrDefault("qdrantCollection", "chatbot");
    }

    public JSONArray search(List<Float> embedding) throws Exception {
        JSONObject body = new JSONObject();
        body.put("vector", new JSONArray(embedding));
        body.put("top", 5); // number of nearest neighbors to return
        body.put("with_payload", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + "/collections/" + collection + "/points/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getJSONArray("result");
        } else {
            throw new Exception("Error querying Qdrant: " + response.body());
        }
    }
}
