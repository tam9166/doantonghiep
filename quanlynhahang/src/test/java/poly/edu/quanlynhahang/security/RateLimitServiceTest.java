package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void blocksRequestsAfterLimitIsExceeded() {
        RateLimitService service = new RateLimitService(
                Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC));

        assertTrue(service.consume("auth:127.0.0.1", 1, 60).allowed());
        assertFalse(service.consume("auth:127.0.0.1", 1, 60).allowed());
    }
}
