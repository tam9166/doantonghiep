package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiDynamicToolServiceTest {
    @Test void capacityQuestionUsesBackendSnapshot() {
        RestaurantCapacityService capacity=mock(RestaurantCapacityService.class); TableAreaRepository areas=mock(TableAreaRepository.class);
        var memory=new AiConversationMemoryService.Memory("session123",20,LocalDate.of(2026,8,17),LocalTime.of(19,0),null,Instant.now());
        when(capacity.checkCapacity(memory.date(),memory.time(),120,20)).thenReturn(new RestaurantCapacityService.CapacitySnapshot(false,200,190,10,20,memory.date(),memory.time(),120));
        String answer=new AiDynamicToolService(capacity,areas).answerAvailabilityQuestion("Còn chỗ không?",memory).orElseThrow();
        assertTrue(answer.contains("10 khách")); verify(capacity).checkCapacity(memory.date(),memory.time(),120,20);
    }
    @Test void requestsMissingDynamicInputsInsteadOfGuessing() {
        var memory=new AiConversationMemoryService.Memory("session123",null,null,null,null,Instant.now());
        String answer=new AiDynamicToolService(mock(RestaurantCapacityService.class),mock(TableAreaRepository.class)).answerAvailabilityQuestion("Tối mai còn chỗ không?",memory).orElseThrow();
        assertTrue(answer.contains("số khách"));
    }
    @Test void sessionMemoryKeepsOnlyBookingFacts() {
        AiConversationMemoryService service=new AiConversationMemoryService();
        var first=service.remember(null,"Tối mai 20 người lúc 19h30 ở sân vườn, email a@b.com");
        var second=service.remember(first.sessionId(),"Còn chỗ không?");
        assertEquals(20,second.guestCount()); assertEquals(LocalTime.of(19,30),second.time()); assertEquals("san vuon",second.area()); assertEquals(LocalDate.now().plusDays(1),second.date());
    }
}
