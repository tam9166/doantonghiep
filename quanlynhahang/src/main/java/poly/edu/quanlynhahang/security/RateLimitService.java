package poly.edu.quanlynhahang.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public RateLimitResult consume(String key, int limit, long windowSeconds) {
        long now = Instant.now(clock).getEpochSecond();
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.windowStart + windowSeconds) {
                return new WindowCounter(now, 1);
            }
            existing.count++;
            return existing;
        });

        long retryAfter = Math.max(1, counter.windowStart + windowSeconds - now);
        return new RateLimitResult(counter.count <= limit, retryAfter);
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    }

    private static final class WindowCounter {
        private final long windowStart;
        private int count;

        private WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
