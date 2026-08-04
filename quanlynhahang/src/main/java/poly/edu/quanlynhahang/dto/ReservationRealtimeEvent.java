package poly.edu.quanlynhahang.dto;

import lombok.Data;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import java.util.Date;

@Data
public class ReservationRealtimeEvent {
    private String eventType;
    private String reservationCode;
    private ReservationStatus oldStatus;
    private ReservationStatus newStatus;
    private Date changedAt;
    private String message;
    private ReservationResponse reservation;
}
