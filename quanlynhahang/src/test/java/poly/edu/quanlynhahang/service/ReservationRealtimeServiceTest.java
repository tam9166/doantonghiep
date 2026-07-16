package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import poly.edu.quanlynhahang.dto.ReservationRealtimeEvent;
import poly.edu.quanlynhahang.dto.ReservationResponse;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReservationRealtimeServiceTest {
    @Test
    void privateReservationTopicNeverContainsReservationPiiPayload() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ReservationRealtimeService service = new ReservationRealtimeService(template);
        ReservationResponse internal = new ReservationResponse();
        internal.setCustomerName("Nguyễn Văn A");
        internal.setCustomerPhone("0912345678");

        service.publish("UPDATED", "MV-001", ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED, "Đã xác nhận", internal);

        ArgumentCaptor<Object> adminPayload = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> privatePayload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/admin/reservations"), adminPayload.capture());
        verify(template).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/reservations/MV-001"), privatePayload.capture());

        ReservationRealtimeEvent adminEvent = (ReservationRealtimeEvent) adminPayload.getValue();
        ReservationRealtimeEvent privateEvent = (ReservationRealtimeEvent) privatePayload.getValue();
        assertSame(internal, adminEvent.getReservation());
        assertNull(privateEvent.getReservation());
        assertEquals("MV-001", privateEvent.getReservationCode());
        assertEquals(ReservationStatus.CONFIRMED, privateEvent.getNewStatus());
    }
}
