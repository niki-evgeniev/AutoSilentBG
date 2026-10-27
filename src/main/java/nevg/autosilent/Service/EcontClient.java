package nevg.autosilent.Service;

import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class EcontClient {
    private final RestClient client;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public EcontClient() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        client = RestClient.builder().requestFactory(factory)
                .baseUrl("https://ee.econt.com/services/Nomenclatures/").build();
    }

    public JsonNode fetch(String method, String key) {
        String body = client.post().uri("NomenclaturesService." + method + ".json")
                .body(Map.of("countryCode", "BGR")).retrieve().body(String.class);
        JsonNode root = mapper.readTree(body);
        JsonNode result = root.path(key);
        if (!result.isArray() || result.isEmpty() || root.hasNonNull("error")) {
            throw new IllegalStateException("Invalid Econt " + key + " response");
        }
        return result;
    }
}
