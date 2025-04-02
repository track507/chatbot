package chatbot.helpers;
import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientPool {
    public static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(HttpClient.Version.HTTP_1_1) 
        .build();
}
