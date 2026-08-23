package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoTableAssignmentServiceTest {
    @Test
    void selectsSmallestAvailableCapacityThenDisplayOrder() {
        RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        RestaurantTable sixSeats = table(1, "B03", 6, 1);
        RestaurantTable fourSeatsLater = table(2, "B02", 4, 5);
        RestaurantTable fourSeatsFirst = table(3, "B01", 4, 2);
        when(tables.findLockedActiveByAreaId(7)).thenReturn(List.of(sixSeats, fourSeatsLater, fourSeatsFirst));
        when(reservations.findLockedByReservationDateAndTableIdAndReservationStatusIn(any(), any(), any()))
                .thenReturn(List.of());

        RestaurantTable selected = new AutoTableAssignmentService(tables, reservations)
                .assign(7, 3, LocalDate.now().plusDays(1), LocalTime.of(18, 0), 120);

        assertEquals(3, selected.getId());
    }

    @Test
    void previousDayOvernightReservationBlocksTheTableAfterMidnight() {
        RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        RestaurantTable onlyTable = table(1, "B01", 4, 1);
        LocalDate date = LocalDate.now().plusDays(2);
        poly.edu.quanlynhahang.entity.Reservation overnight = new poly.edu.quanlynhahang.entity.Reservation();
        overnight.setReservationDate(date.minusDays(1));
        overnight.setArrivalTime(LocalTime.of(23, 30));
        overnight.setExpectedDurationMinutes(120);
        when(tables.findLockedActiveByAreaId(7)).thenReturn(List.of(onlyTable));
        when(reservations.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                eq(date.minusDays(1)), eq(1), any())).thenReturn(List.of(overnight));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> new AutoTableAssignmentService(tables, reservations)
                        .assign(7, 2, date, LocalTime.of(0, 30), 60));
    }

    private RestaurantTable table(int id, String name, int capacity, int order) {
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setName(name);
        table.setMaxCapacity(capacity);
        table.setDisplayOrder(order);
        table.setActive(true);
        table.setIsOccupied(0);
        return table;
    }
}
