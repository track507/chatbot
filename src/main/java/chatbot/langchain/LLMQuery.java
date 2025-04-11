package chatbot.llm;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import chatbot.helpers.Utils;

public class LLMQuery {
    private final Embedder embedder;
    private final OllamaEngine ollama;
    private final QdrantQuery qdrant;
    private final String userProfile;

    public LLMQuery(Embedder embedder, QdrantQuery qdrant) throws Exception {
        this.embedder = embedder;
        this.qdrant = qdrant;

        // get user profile.
        ObjectMapper mapper = new ObjectMapper();
        Utils utils = new Utils();

        Map<String, Object> user = mapper.readValue(
            new File("user_info.json"),
            new com.fasterxml.jackson.core.type.TypeReference<>() {}
        );
        this.userProfile = "User Profile:\n" + utils.formatUserProfile(user);

        this.ollama = new OllamaEngine();
    }

    public String ask(String userInput) throws Exception {
        List<Float> embeddedQuestion = embedder.getEmbedding(userInput);
        JSONArray hits = qdrant.search(embeddedQuestion);

        StringBuilder context = new StringBuilder();
        for(int i = 0; i < hits.length(); i++) {
            JSONObject hit = hits.getJSONObject(i);
            JSONObject payload = hit.optJSONObject("payload");
            if(payload != null && payload.has("content")) {
                context.append(payload.getString("content")).append("\n");
            }
        }

        String fullPrompt = String.format(
            """
            [USER PROFILE]
            %s

            [CONTEXT]
            %s

            Question: %s
            """, userProfile, context.toString(), userInput
        );

        // Debug
        // System.out.println(fullPrompt);

        return ollama.getResponse(fullPrompt);
    }
}