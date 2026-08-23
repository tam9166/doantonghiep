package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI conversation memory service with proper TTL, timezone handling, and bounded caching.
 * P0-3.5: Uses injected Clock + ZoneId instead of LocalDate.now()
 * P0-3.6: Uses bounded map with LRU-style eviction (no more sessions.clear())
 */
@Service
public class AiConversationMemoryService {
    private static final Logger log = LoggerFactory.getLogger(AiConversationMemoryService.class);
    private static final Duration SESSION_TTL = Duration.ofHours(2);
    private static final int MAX_SESSIONS = 5000;
    private static final Pattern GUESTS = Pattern.compile("(?i)(\\d{1,3})\\s*(khách|người|nguoi)");
    private static final Pattern TIME = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])[:h]([0-5]\\d)?");

    private final ZoneId zoneId;
    private final Clock clock;
    private final ConcurrentHashMap<String, Memory> sessions = new ConcurrentHashMap<>();

    public AiConversationMemoryService(
            @Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone,
            Clock clock) {
        this.zoneId = ZoneId.of(timezone);
        this.clock = clock;
    }

    /** Factory bean for testability */
    public static Clock systemClock(ZoneId zone) {
        return Clock.system(zone);
    }

    public Memory remember(String requestedSessionId, String message) {
        String id = resolveSessionId(requestedSessionId);
        Instant now = Instant.now(clock);
        // ComputeIfAbsent for thread-safe session creation
        Memory previous = sessions.compute(id, (k, existing) -> {
            if (existing == null || existing.updatedAt().isBefore(now.minus(SESSION_TTL))) {
                return new Memory(k, null, null, null, null, now);
            }
            return existing;
        });
        
        Integer guests = matchInt(GUESTS, message, previous.guestCount());
        LocalTime time = matchTime(message, previous.time());
        LocalDate date = matchDate(message, previous.date(), now);
        String area = matchArea(message, previous.area());
        Memory updated = new Memory(id, guests, date, time, area, now);
        sessions.put(id, updated);
        
        // LRU-style eviction when near capacity
        if (sessions.size() > MAX_SESSIONS) {
            evictOldestSessions();
        }
        
        return updated;
    }

    private String resolveSessionId(String requestedId) {
        // P0-3.6: If client sends a well-formed sessionId, use it (binds conversation)
        // Otherwise generate a secure random UUID to prevent cross-session pollution
        if (requestedId != null && requestedId.matches("[A-Za-z0-9_-]{8,80}")) {
            return requestedId;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Scheduled cleanup of expired sessions. Never clears ALL sessions at once.
     */
    @Scheduled(fixedDelay = 300_000) // Every 5 minutes
    public void purgeExpiredSessions() {
        Instant cutoff = Instant.now(clock).minus(SESSION_TTL);
        sessions.entrySet().removeIf(e -> e.getValue().updatedAt().isBefore(cutoff));
    }

    /**
     * LRU-style eviction - removes oldest 10% when over capacity
     */
    private void evictOldestSessions() {
        List<Map.Entry<String, Memory>> sorted = new ArrayList<>(sessions.entrySet());
        sorted.sort(Comparator.comparingLong(e -> e.getValue().updatedAt().toEpochMilli()));
        int toRemove = Math.max(100, sorted.size() / 10);
        for (int i = 0; i < toRemove && i < sorted.size(); i++) {
            sessions.remove(sorted.get(i).getKey());
        }
    }

    private Integer matchInt(Pattern pattern, String text, Integer fallback) {
        Matcher m = pattern.matcher(Optional.ofNullable(text).orElse(""));
        return m.find() ? Integer.valueOf(m.group(1)) : fallback;
    }

    private LocalTime matchTime(String text, LocalTime fallback) {
        Matcher m = TIME.matcher(Optional.ofNullable(text).orElse(""));
        if (!m.find()) return fallback;
        return LocalTime.of(Integer.parseInt(m.group(1)),
                m.group(2) == null ? 0 : Integer.parseInt(m.group(2)));
    }

    private LocalDate matchDate(String text, LocalDate fallback, Instant now) {
        String s = normalize(text);
        // P0-3.5: Use injected clock instead of LocalDate.now()
        LocalDate today = LocalDate.ofInstant(now, zoneId);
        if (s.contains("ngay mai") || s.contains("toi mai")) return today.plusDays(1);
        if (s.contains("hom nay") || s.contains("toi nay")) return today;
        Matcher m = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{4}))?").matcher(s);
        if (m.find()) {
            try {
                return LocalDate.of(
                        m.group(3) == null ? today.getYear() : Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(1)));
            } catch (DateTimeException exception) {
                log.debug("Ignoring invalid date candidate in AI conversation: {}", m.group(), exception);
                return fallback;
            }
        }
        return fallback;
    }

    private String matchArea(String text, String fallback) {
        String s = normalize(text);
        for (String area : List.of("san vuon", "phong vip", "san thuong", "sanh su kien")) {
            if (s.contains(area)) return area;
        }
        return fallback;
    }

    private String normalize(String s) {
        return java.text.Normalizer.normalize(Optional.ofNullable(s).orElse(""),
                java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    public record Memory(String sessionId, Integer guestCount, LocalDate date,
                         LocalTime time, String area, Instant updatedAt) {}
}
