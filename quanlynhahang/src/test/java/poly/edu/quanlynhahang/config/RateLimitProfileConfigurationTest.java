package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class RateLimitProfileConfigurationTest {
    @Test
    void baseAndProductionProfilesFailClosed() throws IOException {
        assertEquals("${RATE_LIMIT_ENABLED:true}", property("application.properties", "app.rate-limit.enabled"));
        assertEquals("${RATE_LIMIT_ENABLED:true}", property("application-prod.properties", "app.rate-limit.enabled"));
        assertEquals("${RATE_LIMIT_ENABLED:true}", property("application-production.properties", "app.rate-limit.enabled"));
        assertEquals("${RATE_LIMIT_STORE:database}", property("application-prod.properties", "app.rate-limit.store"));
        assertEquals("${RATE_LIMIT_STORE:database}", property("application-production.properties", "app.rate-limit.store"));
    }

    @Test
    void onlyDevelopmentAndTestProfilesDisableRateLimitingByDefault() throws IOException {
        assertEquals("${RATE_LIMIT_ENABLED:false}", property("application-dev.properties", "app.rate-limit.enabled"));
        assertEquals("${RATE_LIMIT_ENABLED:false}", property("application-test.properties", "app.rate-limit.enabled"));
        assertEquals("${RATE_LIMIT_ENABLED:false}", property("application-local.example.properties", "app.rate-limit.enabled"));
    }

    private String property(String resourceName, String key) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) throw new IOException("Missing resource " + resourceName);
            properties.load(input);
        }
        return properties.getProperty(key);
    }
}
