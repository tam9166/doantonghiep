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

import java.util.List;
import java.util.Map;

@Service
public class GeminiClient {
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final String apiKey;
    private final RestTemplate restTemplate;

    @Autowired
    public GeminiClient(@Value("${gemini.api.key:}") String apiKey) {
        this(apiKey, new RestTemplate());
    }

    GeminiClient(String apiKey, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
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
                ))
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_URL, new HttpEntity<>(body, headers), Map.class);
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
