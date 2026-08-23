package poly.edu.quanlynhahang.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsProfileConfigurationTest {
    @Test
    void productionProfilesNeverFallbackToLocalhost() throws IOException {
        assertEquals("${ALLOWED_ORIGINS:}", property("application-prod.properties"));
        assertEquals("${ALLOWED_ORIGINS:}", property("application-production.properties"));
        assertFalse(property("application-prod.properties").contains("localhost"));
        assertFalse(property("application-production.properties").contains("localhost"));
    }

    @Test
    void localhostOriginsAreConfinedToDevelopmentAndTestProfiles() throws IOException {
        assertTrue(property("application-dev.properties").contains("localhost:5173"));
        assertTrue(property("application-test.properties").contains("localhost:5173"));
    }

    @Test
    void productionProfilesEnableRealCaptchaByDefault() throws IOException {
        for (String profile : new String[] {"application-prod.properties", "application-production.properties"}) {
            Properties properties = properties(profile);
            assertEquals("${CAPTCHA_ENABLED:true}", properties.getProperty("app.captcha.enabled"));
            assertEquals("${CAPTCHA_PROVIDER:turnstile}", properties.getProperty("app.captcha.provider"));
            assertEquals("${CAPTCHA_SECRET:}", properties.getProperty("app.captcha.secret"));
        }
    }

    private String property(String resourceName) throws IOException {
        return properties(resourceName).getProperty("app.cors.allowed-origins");
    }

    private Properties properties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) throw new IOException("Missing resource " + resourceName);
            properties.load(input);
        }
        return properties;
    }
}
