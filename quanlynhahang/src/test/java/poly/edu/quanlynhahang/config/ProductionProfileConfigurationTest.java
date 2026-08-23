package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ProductionProfileConfigurationTest {

    @Test
    void productionTemplateDoesNotAttemptToActivateItself() throws IOException {
        Properties properties = loadProductionTemplate();

        assertNull(properties.getProperty("spring.profiles.active"));
    }

    @Test
    void productionTemplateKeepsSecuritySensitiveDefaultsEnabled() throws IOException {
        Properties properties = loadProductionTemplate();

        assertEquals("${RESTAURANT_PAYMENT_DEMO_MODE:false}",
                properties.getProperty("restaurant.payment.demo-mode"));
        assertEquals("${CAPTCHA_ENABLED:true}", properties.getProperty("app.captcha.enabled"));
        assertEquals("${FORWARD_HEADERS_STRATEGY:none}",
                properties.getProperty("server.forward-headers-strategy"));
    }

    private Properties loadProductionTemplate() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application-production.example.properties")) {
            properties.load(input);
        }
        return properties;
    }
}
