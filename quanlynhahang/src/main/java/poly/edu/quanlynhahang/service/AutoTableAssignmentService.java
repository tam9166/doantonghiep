package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;

@Service
public class AutoTableAssignmentService {
    private static final int CLEANUP_MINUTES = 15;
    private static final EnumSet<ReservationStatus> BLOCKING = EnumSet.of(
            ReservationStatus.PENDING, ReservationStatus.CONFIRMED,
            ReservationStatus.DEPOSIT_REQUIRED, ReservationStatus.DEPOSIT_PENDING,
            ReservationStatus.DEPOSIT_PAID, ReservationStatus.FULLY_PAID,
            ReservationStatus.CHECKED_IN, ReservationStatus.IN_SERVICE);

    private final RestaurantTableRepository tables;
    private final ReservationRepository reservations;

    public AutoTableAssignmentService(RestaurantTableRepository tables, ReservationRepository reservations) {
        this.tables = tables;
        this.reservations = reservations;
    }

    public RestaurantTable assign(Integer areaId, int guests, LocalDate date, LocalTime start, int duration) {
        if (areaId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng chọn khu vực");
        }
        return tables.findLockedActiveByAreaId(areaId).stream()
                .filter(table -> capacity(table) >= guests)
                .filter(table -> table.getIsOccupied() == null || table.getIsOccupied() == 0)
                .filter(table -> !hasConflict(table.getId(), date, start, duration))
                .min(Comparator.comparingInt(this::capacity)
                        .thenComparing(table -> table.getDisplayOrder() == null ? Integer.MAX_VALUE : table.getDisplayOrder())
                        .thenComparing(RestaurantTable::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Khu vực đã chọn không còn bàn đơn phù hợp trong khung giờ này"));
    }

    private boolean hasConflict(Integer tableId, LocalDate date, LocalTime start, int duration) {
        LocalDateTime requestedStart = LocalDateTime.of(date, start);
        LocalDateTime requestedEnd = requestedStart.plusMinutes(duration + CLEANUP_MINUTES);
        java.util.List<Reservation> candidates = new java.util.ArrayList<>(
                reservations.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                        date.minusDays(1), tableId, BLOCKING));
        candidates.addAll(reservations.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                date, tableId, BLOCKING));
        return candidates.stream()
                .anyMatch(existing -> {
                    LocalDateTime otherStart = LocalDateTime.of(
                            existing.getReservationDate(), existing.getArrivalTime());
                    LocalDateTime otherEnd = otherStart.plusMinutes(
                            existing.getExpectedDurationMinutes() + CLEANUP_MINUTES);
                    return requestedStart.isBefore(otherEnd) && requestedEnd.isAfter(otherStart);
                });
    }

    private int capacity(RestaurantTable table) {
        return table.getMaxCapacity() != null ? table.getMaxCapacity()
                : (table.getCapacity() == null ? 0 : table.getCapacity());
    }
}
