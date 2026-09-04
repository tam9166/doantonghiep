package poly.edu.quanlynhahang.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.repository.ReservationRepository;

/** Resolves the real service date and prevents kitchen/waiter work before that date. */
@Service
public class OrderServiceDateGuardService {
    public static final String FUTURE_SERVICE_MESSAGE = "Đơn này chưa đến ngày phục vụ.";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReservationRepository reservationRepository;

    public OrderServiceDateGuardService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public LocalDateTime resolveServiceAt(Order order) {
        if (order == null) return null;
        if (order.getId() != null) {
            var reservation = reservationRepository.findByKitchenOrderId(order.getId()).orElse(null);
            if (reservation != null && reservation.getReservationDate() != null) {
                LocalTime arrival = reservation.getArrivalTime() == null
                        ? LocalTime.MIDNIGHT : reservation.getArrivalTime();
                return LocalDateTime.of(reservation.getReservationDate(), arrival);
            }
        }
        return order.getScheduledAt();
    }

    @Transactional(readOnly = true)
    public void assertServiceDateReached(Order order) {
        LocalDateTime serviceAt = resolveServiceAt(order);
        if (serviceAt != null && serviceAt.toLocalDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, FUTURE_SERVICE_MESSAGE);
        }
    }
}
