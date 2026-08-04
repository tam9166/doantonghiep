package poly.edu.quanlynhahang.security;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class ConfigurableCaptchaVerifier implements CaptchaVerifier {

    private static final String TURNSTILE_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String RECAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final boolean enabled;
    private final String provider;
    private final String secret;
    private final String mockToken;
    private final double minScore;
    private final String verifyUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public ConfigurableCaptchaVerifier(@Value("${app.captcha.enabled:false}") boolean enabled,
                                       @Value("${app.captcha.provider:mock}") String provider,
                                       @Value("${app.captcha.secret:}") String secret,
                                       @Value("${app.captcha.mock-token:}") String mockToken,
                                       @Value("${app.captcha.min-score:0.5}") double minScore,
                                       @Value("${app.captcha.verify-url:}") String verifyUrl) {
        this(enabled, provider, secret, mockToken, minScore, verifyUrl, new RestTemplate());
    }

    ConfigurableCaptchaVerifier(boolean enabled,
                                String provider,
                                String secret,
                                String mockToken,
                                double minScore,
                                String verifyUrl,
                                RestTemplate restTemplate) {
        this.enabled = enabled;
        this.provider = normalize(provider);
        this.secret = secret == null ? "" : secret.trim();
        this.mockToken = mockToken == null ? "" : mockToken.trim();
        this.minScore = minScore;
        this.verifyUrl = verifyUrl == null ? "" : verifyUrl.trim();
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean verify(String token, String clientIp, String action) {
        if (!enabled) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        if ("mock".equals(provider)) {
            return !mockToken.isBlank() && mockToken.equals(token);
        }
        if (secret.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> body = callProvider(token, clientIp);
            return isAccepted(body, action);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callProvider(String token, String clientIp) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        if (clientIp != null && !clientIp.isBlank()) {
            form.add("remoteip", clientIp);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.postForEntity(resolveVerifyUrl(), new HttpEntity<>(form, headers), Map.class);
        return response.getBody() == null ? Map.of() : (Map<String, Object>) response.getBody();
    }

    private String resolveVerifyUrl() {
        if (!verifyUrl.isBlank()) {
            return verifyUrl;
        }
        if ("recaptcha".equals(provider)) {
            return RECAPTCHA_URL;
        }
        return TURNSTILE_URL;
    }

    private boolean isAccepted(Map<String, Object> body, String expectedAction) {
        if (!Boolean.TRUE.equals(body.get("success"))) {
            return false;
        }
        if ("recaptcha".equals(provider)) {
            Object score = body.get("score");
            if (score instanceof Number number && number.doubleValue() < minScore) {
                return false;
            }
        }
        Object providerAction = body.get("action");
        return providerAction == null
                || expectedAction == null
                || expectedAction.isBlank()
                || providerAction.toString().equals(expectedAction);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("turnstile".equals(normalized) || "recaptcha".equals(normalized)) {
            return normalized;
        }
        return "mock";
    }
}
