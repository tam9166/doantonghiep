package poly.edu.quanlynhahang.service;

import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiClient {
    // P0-08: Model name now configurable via GEMINI_MODEL env var (default: gemini-2.5-flash)
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";

    private final String apiKey;
    private final String modelName;
    private final RestTemplate restTemplate;

    @Autowired
    public GeminiClient(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:" + DEFAULT_MODEL + "}") String modelName) {
        this(apiKey, modelName, createSecureRestTemplate());
    }

    GeminiClient(String apiKey, String modelName, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.modelName = modelName != null && !modelName.isBlank() ? modelName : DEFAULT_MODEL;
        this.restTemplate = restTemplate;
    }

    /**
     * Create RestTemplate with strict timeouts to prevent hanging calls
     */
    private static RestTemplate createSecureRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    public String generate(String prompt, String useCase) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_GEMINI_API_KEY_HERE".equals(apiKey.trim())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED");
        }

        Metrics.counter("restaurant.ai.requests", "use_case", useCase).increment();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey.trim());
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024
                )
        );

        try {
            String url = API_URL_TEMPLATE.replace("{model}", modelName);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);
            String reply = extractReply(response.getBody());
            Metrics.counter("restaurant.ai.responses", "use_case", useCase, "result", "success").increment();
            return reply.replace("**", "");
        } catch (HttpClientErrorException.TooManyRequests exception) {
            Metrics.counter("restaurant.ai.responses", "use_case", useCase, "result", "quota_exceeded").increment();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI_QUOTA_EXCEEDED", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            Metrics.counter("restaurant.ai.responses", "use_case", useCase, "result", "error").increment();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", exception);
        }
    }

    private String extractReply(Map<?, ?> body) {
        try {
            List<?> candidates = (List<?>) body.get("candidates");
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.getFirst();
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            String reply = (String) ((Map<?, ?>) parts.getFirst()).get("text");
            if (reply == null || reply.isBlank()) throw new IllegalArgumentException("Gemini reply is empty");
            return reply;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", exception);
        }
    }
}
