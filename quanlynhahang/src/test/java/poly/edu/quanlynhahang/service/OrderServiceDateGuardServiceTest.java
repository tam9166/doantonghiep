package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.ReservationStatusHistory;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.ReservationStatusHistoryRepository;

class OrderServiceDateGuardServiceTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ReservationStatusHistoryRepository histories = mock(ReservationStatusHistoryRepository.class);
    private final OrderServiceDateGuardService service = new OrderServiceDateGuardService(reservations, histories);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void reservationDateIsCanonicalAndFutureDateIsRejected() {
        Order order = new Order();
        order.setId(42);
        order.setScheduledAt(LocalDateTime.now().minusDays(2));
        Reservation reservation = new Reservation();
        reservation.setId(42L);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setArrivalTime(LocalTime.of(18, 0));
        when(reservations.findByKitchenOrderId(42)).thenReturn(Optional.of(reservation));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assertServiceDateReached(order));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(OrderServiceDateGuardService.FUTURE_SERVICE_MESSAGE, error.getReason());
        assertEquals(LocalDateTime.of(reservation.getReservationDate(), reservation.getArrivalTime()),
                service.resolveServiceAt(order));
    }

    @Test
    void todayPreorderIsBlockedUntilThirtyMinutesBeforeArrival() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        Order order = preorderOrder(43, now.plusMinutes(45));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assertServiceDateReached(order));

        assertEquals(OrderServiceDateGuardService.BEFORE_PREPARATION_MESSAGE, error.getReason());
        org.junit.jupiter.api.Assertions.assertFalse(service.isPreparationReached(order));
    }

    @Test
    void reachedPreparationWindowIsOperable() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        Order order = preorderOrder(44, now.plusMinutes(29));

        assertDoesNotThrow(() -> service.assertServiceDateReached(order));
        org.junit.jupiter.api.Assertions.assertTrue(service.isPreparationReached(order));
    }

    @Test
    void earlyCheckInStillWaitsUntilScheduledPreparationLeadTime() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        Order order = preorderOrder(45, now.plusHours(1));
        ReservationStatusHistory checkIn = checkInHistory(now.minusMinutes(5));
        when(histories.findFirstByReservationIdAndNewStatusOrderByChangedAtAsc(45L, ReservationStatus.CHECKED_IN))
                .thenReturn(Optional.of(checkIn));

        var timing = service.resolveTiming(order);

        assertEquals(now.plusMinutes(30), timing.prepareStartTime().withNano(0));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assertServiceDateReached(order));
        assertEquals(OrderServiceDateGuardService.BEFORE_PREPARATION_MESSAGE, error.getReason());
    }

    @Test
    void lateCheckInUsesActualCheckInInsteadOfScheduledMinusThirtyMinutes() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        Order order = preorderOrder(46, now.minusMinutes(10));
        ReservationStatusHistory checkIn = checkInHistory(now.minusMinutes(2));
        when(histories.findFirstByReservationIdAndNewStatusOrderByChangedAtAsc(46L, ReservationStatus.CHECKED_IN))
                .thenReturn(Optional.of(checkIn));

        assertEquals(now.minusMinutes(2), service.resolveTiming(order).prepareStartTime().withNano(0));
    }

    @Test
    void ordinaryAndUnscheduledOrdersRemainOperable() {
        Order ordinary = new Order();
        ordinary.setScheduledAt(LocalDate.now().plusDays(1).atTime(23, 0));
        Order unscheduled = new Order();

        assertDoesNotThrow(() -> service.assertServiceDateReached(ordinary));
        assertDoesNotThrow(() -> service.assertServiceDateReached(unscheduled));
    }

    private Order preorderOrder(int orderId, LocalDateTime serviceAt) {
        Order order = new Order();
        order.setId(orderId);
        order.setScheduledAt(serviceAt.minusDays(3));
        Reservation reservation = new Reservation();
        reservation.setId((long) orderId);
        reservation.setReservationDate(serviceAt.toLocalDate());
        reservation.setArrivalTime(serviceAt.toLocalTime());
        when(reservations.findByKitchenOrderId(orderId)).thenReturn(Optional.of(reservation));
        return order;
    }

    private ReservationStatusHistory checkInHistory(LocalDateTime value) {
        ReservationStatusHistory history = new ReservationStatusHistory();
        history.setNewStatus(ReservationStatus.CHECKED_IN);
        history.setChangedAt(Date.from(value.atZone(BUSINESS_ZONE).toInstant()));
        return history;
    }
}
