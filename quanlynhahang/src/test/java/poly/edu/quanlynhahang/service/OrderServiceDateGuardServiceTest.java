package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.repository.ReservationRepository;

class OrderServiceDateGuardServiceTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final OrderServiceDateGuardService service = new OrderServiceDateGuardService(reservations);

    @Test
    void reservationDateIsCanonicalAndFutureDateIsRejected() {
        Order order = new Order();
        order.setId(42);
        order.setScheduledAt(LocalDateTime.now().minusDays(2));
        Reservation reservation = new Reservation();
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
    void todayAndUnscheduledOrdersRemainOperable() {
        Order today = new Order();
        today.setScheduledAt(LocalDate.now().atTime(23, 0));
        Order unscheduled = new Order();

        assertDoesNotThrow(() -> service.assertServiceDateReached(today));
        assertDoesNotThrow(() -> service.assertServiceDateReached(unscheduled));
    }
}
