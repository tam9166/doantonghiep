package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for AiConversationMemoryService with injected Clock.
 */
@SpringBootTest
class AiConversationMemoryServiceTest {
    
    @Autowired(required = false)
    private Clock systemClock;
    
    @Test
    void extractsGuestCountFromVietnameseText() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        var memory = service.remember(null, "Tôi muốn đặt bàn cho 10 người");
        
        assertNotNull(memory);
        assertEquals(Integer.valueOf(10), memory.guestCount());
    }
    
    @Test
    void extractsTimeFromVietnameseText() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        var memory = service.remember(null, "Đặt bàn lúc 19h30");
        
        assertNotNull(memory);
        assertEquals(java.time.LocalTime.of(19, 30), memory.time());
    }
    
    @Test
    void extractsDateTomorrow() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        var memory = service.remember(null, "Tối mai tôi muốn đặt bàn");
        
        assertNotNull(memory);
        assertEquals(java.time.LocalDate.of(2026, 8, 20), memory.date());
    }
    
    @Test
    void extractsAreaFromText() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        var memory = service.remember(null, "Tôi muốn ngồi ở sân vườn");
        
        assertNotNull(memory);
        assertEquals("san vuon", memory.area());
    }
    
    @Test
    void persistsSessionAcrossCalls() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        var first = service.remember("user-session-1", "20 người lúc 19h");
        var second = service.remember("user-session-1", "Tôi cần bànVIP");
        
        // Should persist the guest count from first call
        assertEquals(Integer.valueOf(20), second.guestCount());
        assertEquals("user-session-1", second.sessionId());
    }
    
    @Test
    void generatesNewSessionIdForNullInput() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        var memory = service.remember(null, "Cho tôi xem menu");
        
        assertNotNull(memory.sessionId());
        assertFalse(memory.sessionId().isBlank());
    }
    
    @Test
    void validatesWellFormedSessionId() {
        Clock testClock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        AiConversationMemoryService service = new AiConversationMemoryService("Asia/Ho_Chi_Minh", testClock);
        
        // Valid session ID format (8-80 alphanumeric chars)
        var memory1 = service.remember("abc12345", "message");
        assertEquals("abc12345", memory1.sessionId());
        
        // Invalid session ID (too short) - should generate UUID
        var memory2 = service.remember("ab", "message");
        assertNotEquals("ab", memory2.sessionId());
    }
}
