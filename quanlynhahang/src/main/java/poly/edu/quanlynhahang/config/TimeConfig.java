package poly.edu.quanlynhahang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuration for time-aware beans using injected Clock + ZoneId.
 */
@Configuration
public class TimeConfig {
    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    /**
     * Provides a Clock with configurable timezone for all time-dependent services.
     * This ensures AI, reservations, and other time-sensitive operations use the correct zone.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }
}
