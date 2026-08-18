package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P0-3.2: Real availability check - combines capacity, tables, and reservations
 * instead of just calling checkCapacity().
 */
@Service
public class AiAvailabilityToolService {
    private static final int DEFAULT_DURATION_MINUTES = 120;
    private static final int CLEANUP_MINUTES = 15;

    private final RestaurantCapacityService capacityService;
    private final RestaurantTableRepository tableRepository;
    private final TableAreaRepository areaRepository;
    private final ReservationRepository reservationRepository;

    public AiAvailabilityToolService(
            RestaurantCapacityService capacityService,
            RestaurantTableRepository tableRepository,
            TableAreaRepository areaRepository,
            ReservationRepository reservationRepository) {
        this.capacityService = capacityService;
        this.tableRepository = tableRepository;
        this.areaRepository = areaRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Get detailed availability snapshot for AI to reason about.
     */
    public AvailabilitySnapshot getAvailability(LocalDate date, LocalTime time, int guestCount) {
        var snapshot = capacityService.checkCapacity(date, time, DEFAULT_DURATION_MINUTES, guestCount);
        
        // Get actual available tables (not just capacity)
        List<TableInfo> availableTables = findAvailableTables(date, time, guestCount);
        
        // Get area info
        List<String> suitableAreas = findSuitableAreas(guestCount);
        
        return new AvailabilitySnapshot(
                snapshot.available(),
                snapshot.maximumCapacity(),
                snapshot.occupiedGuests(),
                snapshot.remainingCapacity(),
                guestCount,
                date,
                time,
                DEFAULT_DURATION_MINUTES,
                availableTables,
                suitableAreas
        );
    }

    /**
     * Find specific tables that can accommodate the guest count at the given time.
     */
    private List<TableInfo> findAvailableTables(LocalDate date, LocalTime time, int guestCount) {
        return tableRepository.findAll().stream()
                .filter(RestaurantTable::getActive)
                .sorted(Comparator.comparing(RestaurantTable::getAreaId, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RestaurantTable::getId))
                .filter(table -> {
                    if (table.getMaxCapacity() == null || table.getMaxCapacity() < guestCount) {
                        return false;
                    }
                    // Check if table is already reserved at this time
                    boolean hasConflict = reservationRepository
                            .findByTableIdAndReservationDateAndReservationStatusIn(
                                    table.getId(), date, 
                                    Set.of(ReservationStatus.CONFIRMED, ReservationStatus.DEPOSIT_REQUIRED,
                                           ReservationStatus.DEPOSIT_PENDING, ReservationStatus.DEPOSIT_PAID,
                                           ReservationStatus.CHECKED_IN, ReservationStatus.IN_SERVICE))
                            .stream()
                            .anyMatch(r -> hasTimeOverlap(time, DEFAULT_DURATION_MINUTES, r));
                    return !hasConflict;
                })
                .map(table -> new TableInfo(
                        table.getId(),
                        table.getName(),
                        table.getCapacity(),
                        table.getMaxCapacity(),
                        table.getAreaId()))
                .limit(10)
                .toList();
    }

    private boolean hasTimeOverlap(LocalTime start, int durationMinutes, 
                                   poly.edu.quanlynhahang.entity.Reservation existing) {
        LocalTime otherStart = existing.getArrivalTime();
        LocalTime otherEnd = otherStart.plusMinutes(existing.getExpectedDurationMinutes() + CLEANUP_MINUTES);
        LocalTime end = start.plusMinutes(durationMinutes + CLEANUP_MINUTES);
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    private List<String> findSuitableAreas(int guestCount) {
        return areaRepository.findByStatus("ACTIVE").stream()
                .filter(a -> (a.getMinGuestCount() == null || a.getMinGuestCount() <= guestCount) &&
                             (a.getMaxGuestCount() == null || a.getMaxGuestCount() >= guestCount))
                .map(a -> a.getNameVi())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public record TableInfo(Integer id, String name, Integer capacity, Integer maxCapacity, Integer areaId) {}
    
    public record AvailabilitySnapshot(
            boolean available,
            int maximumCapacity,
            int occupiedGuests,
            int remainingCapacity,
            int requestedGuests,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            List<TableInfo> availableTables,
            List<String> suitableAreas
    ) {}
}
