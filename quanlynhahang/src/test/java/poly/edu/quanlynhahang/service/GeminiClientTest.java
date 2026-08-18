package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {
    @Test
    void sendsApiKeyInHeaderInsteadOfQueryString() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        // Updated URL template with model placeholder
        server.expect(once(), requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello **AI**\"}]}}]}",
                        MediaType.APPLICATION_JSON));

        // Use new constructor with configurable model name
        GeminiClient client = new GeminiClient("secret-key", "gemini-2.5-flash", restTemplate);

        assertEquals("Hello AI", client.generate("prompt", "SUPPORT"));
        server.verify();
    }
    
    @Test
    void usesConfigurableModelName() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        
        String customModel = "gemini-2.5-pro";
        server.expect(once(), requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}",
                        MediaType.APPLICATION_JSON));

        GeminiClient client = new GeminiClient("test-key", customModel, restTemplate);
        
        assertEquals("OK", client.generate("prompt", "SUPPORT"));
        server.verify();
    }
}
