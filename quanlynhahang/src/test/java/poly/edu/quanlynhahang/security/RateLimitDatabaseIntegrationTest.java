package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
class RateLimitDatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void separateServiceInstancesShareTheSameQuota() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC);
        RateLimitService firstInstance = new RateLimitService(clock, jdbcTemplate, transactionManager, true);
        RateLimitService secondInstance = new RateLimitService(clock, jdbcTemplate, transactionManager, true);
        String key = "regression:" + UUID.randomUUID();
        try {
            assertTrue(firstInstance.consume(key, 1, 60).allowed());
            assertFalse(secondInstance.consume(key, 1, 60).allowed());
        } finally {
            jdbcTemplate.update("DELETE FROM dbo.api_rate_limits WHERE bucket_key = ?", key);
        }
    }
}
