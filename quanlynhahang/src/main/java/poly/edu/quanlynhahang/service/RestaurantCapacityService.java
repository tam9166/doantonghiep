package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

@Service
public class RestaurantCapacityService {
    private static final int CLEANUP_MINUTES = 15;
    private static final EnumSet<ReservationStatus> BLOCKING = EnumSet.of(
            ReservationStatus.PENDING, ReservationStatus.WAITING_TABLE_ASSIGNMENT,
            ReservationStatus.CONFIRMED, ReservationStatus.DEPOSIT_REQUIRED,
            ReservationStatus.DEPOSIT_PENDING, ReservationStatus.DEPOSIT_PAID,
            ReservationStatus.FULLY_PAID, ReservationStatus.CHECKED_IN,
            ReservationStatus.IN_SERVICE);

    private final ReservationRepository reservations;
    private final RestaurantSettingsService settings;

    public RestaurantCapacityService(ReservationRepository reservations, RestaurantSettingsService settings) {
        this.reservations = reservations;
        this.settings = settings;
    }

    /** Called inside the booking transaction; locking the singleton setting row serializes capacity decisions. */
    public void requireCapacity(LocalDate date, LocalTime start, int durationMinutes, int requestedGuests) {
        int maximum = Integer.parseInt(settings.lockCapacitySetting().getValue().trim());
        LocalTime end = start.plusMinutes(durationMinutes + CLEANUP_MINUTES);
        int occupied = reservations.findByReservationDateAndReservationStatusIn(date, BLOCKING).stream()
                .filter(existing -> overlaps(start, end, existing))
                .mapToInt(Reservation::getGuestCount)
                .sum();
        int remaining = Math.max(0, maximum - occupied);
        if (requestedGuests > remaining) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khung giờ Quý khách chọn hiện không còn đủ sức chứa cho " + requestedGuests
                            + " khách. Nhà hàng hiện chỉ còn khả năng tiếp nhận tối đa " + remaining
                            + " khách trong khoảng thời gian này.");
        }
    }

    public CapacitySnapshot checkCapacity(LocalDate date, LocalTime start, int durationMinutes, int requestedGuests) {
        int maximum = settings.maxCapacity();
        LocalTime end = start.plusMinutes(durationMinutes + CLEANUP_MINUTES);
        int occupied = reservations.findByReservationDateAndReservationStatusIn(date, BLOCKING).stream()
                .filter(existing -> overlaps(start, end, existing)).mapToInt(Reservation::getGuestCount).sum();
        int remaining = Math.max(0, maximum - occupied);
        return new CapacitySnapshot(requestedGuests <= remaining, maximum, occupied, remaining, requestedGuests, date, start, durationMinutes);
    }

    public record CapacitySnapshot(boolean available, int maximumCapacity, int occupiedGuests, int remainingCapacity,
                                   int requestedGuests, LocalDate date, LocalTime startTime, int durationMinutes) {}

    private boolean overlaps(LocalTime start, LocalTime end, Reservation existing) {
        LocalTime otherStart = existing.getArrivalTime();
        LocalTime otherEnd = otherStart.plusMinutes(existing.getExpectedDurationMinutes() + CLEANUP_MINUTES);
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }
}
