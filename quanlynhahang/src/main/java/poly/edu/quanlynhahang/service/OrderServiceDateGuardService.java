package poly.edu.quanlynhahang.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.ReservationStatusHistoryRepository;

/** Canonical timing authority for reservation preorders in Kitchen and Waiter flows. */
@Service
public class OrderServiceDateGuardService {
    public static final String FUTURE_SERVICE_MESSAGE = "Đơn này chưa đến ngày phục vụ.";
    public static final String BEFORE_PREPARATION_MESSAGE = "Đơn đặt trước chưa đến thời gian chuẩn bị.";
    public static final long PREPARATION_LEAD_MINUTES = 30;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReservationRepository reservationRepository;
    private final ReservationStatusHistoryRepository historyRepository;

    public OrderServiceDateGuardService(ReservationRepository reservationRepository,
                                        ReservationStatusHistoryRepository historyRepository) {
        this.reservationRepository = reservationRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public LocalDateTime resolveServiceAt(Order order) {
        PreparationTiming timing = resolveTiming(order);
        return timing == null ? null : timing.serviceAt();
    }

    @Transactional(readOnly = true)
    public PreparationTiming resolveTiming(Order order) {
        if (order == null) return null;
        if (order.getId() != null) {
            var reservation = reservationRepository.findByKitchenOrderId(order.getId()).orElse(null);
            if (reservation != null && reservation.getReservationDate() != null) {
                LocalTime arrival = reservation.getArrivalTime() == null
                        ? LocalTime.MIDNIGHT : reservation.getArrivalTime();
                LocalDateTime serviceAt = LocalDateTime.of(reservation.getReservationDate(), arrival);
                LocalDateTime checkedInAt = historyRepository
                        .findFirstByReservationIdAndNewStatusOrderByChangedAtAsc(
                                reservation.getId(), ReservationStatus.CHECKED_IN)
                        .map(history -> toBusinessDateTime(history.getChangedAt()))
                        .orElseGet(() -> ReservationStatus.CHECKED_IN.equals(reservation.getReservationStatus())
                                ? toBusinessDateTime(reservation.getUpdatedAt()) : null);
                // Check-in can happen up to one hour early, but a preorder must not enter
                // preparation before the normal 30-minute lead window.
                LocalDateTime scheduledPrepareStart = serviceAt.minusMinutes(PREPARATION_LEAD_MINUTES);
                LocalDateTime prepareStartTime = checkedInAt != null
                        ? laterOf(checkedInAt, scheduledPrepareStart)
                        : scheduledPrepareStart;
                return new PreparationTiming(serviceAt, prepareStartTime, checkedInAt, true);
            }
        }
        return new PreparationTiming(order.getScheduledAt(), null, null, false);
    }

    @Transactional(readOnly = true)
    public boolean isPreparationReached(Order order) {
        PreparationTiming timing = resolveTiming(order);
        return timing == null || !timing.preorder() || timing.prepareStartTime() == null
                || !LocalDateTime.now(BUSINESS_ZONE).isBefore(timing.prepareStartTime());
    }

    @Transactional(readOnly = true)
    public void assertPreparationReached(Order order) {
        PreparationTiming timing = resolveTiming(order);
        if (timing == null || !timing.preorder() || timing.prepareStartTime() == null) return;
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (now.isBefore(timing.prepareStartTime())) {
            String message = timing.serviceAt() != null
                    && timing.serviceAt().toLocalDate().isAfter(LocalDate.now(BUSINESS_ZONE))
                    ? FUTURE_SERVICE_MESSAGE : BEFORE_PREPARATION_MESSAGE;
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    /** Kept for callers compiled against the former date-only guard. */
    @Transactional(readOnly = true)
    public void assertServiceDateReached(Order order) {
        assertPreparationReached(order);
    }

    private LocalDateTime toBusinessDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), BUSINESS_ZONE);
    }

    private LocalDateTime laterOf(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    public record PreparationTiming(LocalDateTime serviceAt, LocalDateTime prepareStartTime,
                                    LocalDateTime checkedInAt, boolean preorder) {
    }
}
