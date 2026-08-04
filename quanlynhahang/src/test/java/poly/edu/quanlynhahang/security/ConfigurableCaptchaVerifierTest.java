package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurableCaptchaVerifierTest {

    @Test
    void disabledCaptchaAllowsRequest() {
        ConfigurableCaptchaVerifier verifier = new ConfigurableCaptchaVerifier(
                false, "turnstile", "", "", 0.5, "", new RestTemplate());

        assertTrue(verifier.verify(null, "127.0.0.1", "auth"));
    }

    @Test
    void mockCaptchaRequiresExactToken() {
        ConfigurableCaptchaVerifier verifier = new ConfigurableCaptchaVerifier(
                true, "mock", "", "local-token", 0.5, "", new RestTemplate());

        assertTrue(verifier.verify("local-token", "127.0.0.1", "auth"));
        assertFalse(verifier.verify("bad-token", "127.0.0.1", "auth"));
    }

    @Test
    void realProviderWithoutSecretRejectsRequest() {
        ConfigurableCaptchaVerifier verifier = new ConfigurableCaptchaVerifier(
                true, "turnstile", "", "", 0.5, "", new RestTemplate());

        assertFalse(verifier.verify("captcha-token", "127.0.0.1", "auth"));
    }

    @Test
    void realProviderRejectsTokenIssuedForAnotherAction() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(
                new ResponseEntity<>(Map.of("success", true, "action", "reservation-create"), HttpStatus.OK));
        ConfigurableCaptchaVerifier verifier = new ConfigurableCaptchaVerifier(
                true, "turnstile", "configured-secret", "", 0.5, "", restTemplate);

        assertFalse(verifier.verify("captcha-token", "127.0.0.1", "reservation-review-create"));
    }
}
