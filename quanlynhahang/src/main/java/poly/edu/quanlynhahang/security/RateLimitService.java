package poly.edu.quanlynhahang.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P0: Rate limit service using Caffeine cache with auto-expiry.
 * Avoids memory leak from ConcurrentHashMap (keys never removed).
 * Thread-safe, bounded, multi-instance ready (can swap to Redis).
 */
@Service
public class RateLimitService {

    private final Cache<String, WindowCounter> counters;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final boolean databaseStore;
    private final AtomicLong databaseOperations = new AtomicLong();

    public RateLimitService() {
        this(Clock.systemUTC(), null, null, false);
    }

    RateLimitService(Clock clock) {
        this(clock, null, null, false);
    }

    @Autowired
    public RateLimitService(JdbcTemplate jdbcTemplate,
                            PlatformTransactionManager transactionManager,
                            @Value("${app.rate-limit.store:memory}") String store) {
        this(Clock.systemUTC(), jdbcTemplate, transactionManager, "database".equalsIgnoreCase(store));
    }

    RateLimitService(Clock clock,
                     JdbcTemplate jdbcTemplate,
                     PlatformTransactionManager transactionManager,
                     boolean databaseStore) {
        this.clock = clock;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        this.databaseStore = databaseStore;
        // Caffeine: tự động xóa entries sau 1 giờ không truy cập
        // Giới hạn 100k entries tối đa
        this.counters = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    public RateLimitResult consume(String key, int limit, long windowSeconds) {
        long now = Instant.now(clock).getEpochSecond();
        if (databaseStore) {
            return consumeDatabase(key, limit, windowSeconds, now);
        }
        WindowCounter counter = counters.get(key, k -> new WindowCounter(now, 0));
        int count;
        synchronized (counter) {
            if (now >= counter.windowStart + windowSeconds) {
                counter.windowStart = now;
                counter.count = 1;
                count = 1;
            } else {
                counter.count++;
                count = counter.count;
            }
        }
        long retryAfter = Math.max(1, counter.windowStart + windowSeconds - now);
        return new RateLimitResult(count <= limit, retryAfter);
    }

    private RateLimitResult consumeDatabase(String key, int limit, long windowSeconds, long now) {
        if (jdbcTemplate == null || transactionTemplate == null) {
            throw new IllegalStateException("Database rate-limit store is not configured");
        }
        DatabaseCounter counter = transactionTemplate.execute(status -> jdbcTemplate.queryForObject("""
                MERGE dbo.api_rate_limits WITH (HOLDLOCK) AS target
                USING (SELECT CAST(? AS varchar(220)) AS bucket_key) AS source
                   ON target.bucket_key = source.bucket_key
                WHEN MATCHED THEN
                    UPDATE SET
                        window_end_epoch = CASE WHEN ? >= target.window_end_epoch THEN ? ELSE target.window_end_epoch END,
                        request_count = CASE WHEN ? >= target.window_end_epoch THEN 1 ELSE target.request_count + 1 END,
                        updated_at = SYSUTCDATETIME()
                WHEN NOT MATCHED THEN
                    INSERT (bucket_key, window_end_epoch, request_count, updated_at)
                    VALUES (source.bucket_key, ?, 1, SYSUTCDATETIME())
                OUTPUT inserted.window_end_epoch, inserted.request_count;
                """, (rs, rowNum) -> new DatabaseCounter(rs.getLong(1), rs.getInt(2)),
                key, now, now + windowSeconds, now, now + windowSeconds));
        if ((databaseOperations.incrementAndGet() & 255L) == 0L) {
            cleanupDatabase(now);
        }
        long retryAfter = Math.max(1, counter.windowEndEpoch - now);
        return new RateLimitResult(counter.count <= limit, retryAfter);
    }

    private void cleanupDatabase(long now) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("DELETE FROM dbo.api_rate_limits WHERE window_end_epoch < ?", now - 3600);
            Long size = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.api_rate_limits", Long.class);
            if (size != null && size > 100_000L) {
                int excess = Math.toIntExact(Math.min(size - 100_000L, Integer.MAX_VALUE));
                jdbcTemplate.update("""
                        DELETE FROM dbo.api_rate_limits
                        WHERE bucket_key IN (
                            SELECT TOP (?) bucket_key FROM dbo.api_rate_limits ORDER BY updated_at ASC
                        )
                        """, excess);
            }
        });
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    }

    private static final class WindowCounter {
        private long windowStart;
        private int count;

        private WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    private record DatabaseCounter(long windowEndEpoch, int count) {
    }
}
