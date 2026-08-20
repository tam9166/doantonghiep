package poly.edu.quanlynhahang.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupValidator.class);
    private final Environment environment;
    private final PaymentProperties paymentProperties;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${restaurant.payment.webhook-secret:}")
    private String paymentWebhookSecret;

    public ApplicationStartupValidator(Environment environment, PaymentProperties paymentProperties) {
        this.environment = environment;
        this.paymentProperties = paymentProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile)
                        || "production".equalsIgnoreCase(profile)
                        || "stage".equalsIgnoreCase(profile)
                        || "staging".equalsIgnoreCase(profile));

        if (hasText(jwtSecret) && jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters for HS256 signing.");
        }

        if (!production) {
            if (!hasText(jwtSecret)) {
                log.warn("JWT_SECRET is not configured. The stable development-only signing key is active; configure JWT_SECRET before production.");
            }
            return;
        }

        requireSecret(jwtSecret, "JWT_SECRET");
        requireSecret(paymentWebhookSecret, "PAYMENT_WEBHOOK_SECRET");
        paymentProperties.assertProductionReady();
    }

    private void requireSecret(String value, String name) {
        if (!hasText(value)) {
            throw new IllegalStateException(name + " is required when running with a production-like profile.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
