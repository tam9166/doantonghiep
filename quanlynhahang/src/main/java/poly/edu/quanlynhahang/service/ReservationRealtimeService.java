package poly.edu.quanlynhahang.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.dto.ReservationRealtimeEvent;
import poly.edu.quanlynhahang.dto.ReservationResponse;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import java.util.Date;

@Service
public class ReservationRealtimeService {
    private final SimpMessagingTemplate messagingTemplate;

    public ReservationRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String eventType,
                        String reservationCode,
                        ReservationStatus oldStatus,
                        ReservationStatus newStatus,
                        String message,
                        ReservationResponse reservation) {
        ReservationRealtimeEvent event = new ReservationRealtimeEvent();
        event.setEventType(eventType);
        event.setReservationCode(reservationCode);
        event.setOldStatus(oldStatus);
        event.setNewStatus(newStatus);
        event.setChangedAt(new Date());
        event.setMessage(message);
        event.setReservation(reservation);

        messagingTemplate.convertAndSend("/topic/admin/reservations", event);

        ReservationRealtimeEvent privateEvent = new ReservationRealtimeEvent();
        privateEvent.setEventType(eventType);
        privateEvent.setReservationCode(reservationCode);
        privateEvent.setOldStatus(oldStatus);
        privateEvent.setNewStatus(newStatus);
        privateEvent.setChangedAt(event.getChangedAt());
        privateEvent.setMessage(message);
        privateEvent.setReservation(null);
        messagingTemplate.convertAndSend("/topic/reservations/" + reservationCode, privateEvent);
    }
}
