package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.RestaurantSetting;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestaurantCapacityServiceTest {
    @Test
    void rejectsOverlappingGuestsBeyondConfiguredMaximum() {
        ReservationRepository reservations = mock(ReservationRepository.class);
        RestaurantSettingsService settings = mock(RestaurantSettingsService.class);
        RestaurantSetting maximum = new RestaurantSetting();
        maximum.setValue("200");
        when(settings.lockCapacitySetting()).thenReturn(maximum);
        LocalDate date = LocalDate.now().plusDays(1);
        Reservation existing = reservation(date, 190, LocalTime.of(18, 30), 120);
        when(reservations.findByReservationDateAndReservationStatusIn(eq(date), any())).thenReturn(List.of(existing));
        RestaurantCapacityService service = new RestaurantCapacityService(reservations, settings);

        assertThrows(ResponseStatusException.class,
                () -> service.requireCapacity(date, LocalTime.of(19, 0), 120, 20));
        assertDoesNotThrow(() -> service.requireCapacity(date, LocalTime.of(21, 0), 60, 20));
    }

    @Test
    void previousDayOvernightReservationConsumesNextDayCapacity() {
        ReservationRepository reservations = mock(ReservationRepository.class);
        RestaurantSettingsService settings = mock(RestaurantSettingsService.class);
        RestaurantSetting maximum = new RestaurantSetting();
        maximum.setValue("10");
        when(settings.lockCapacitySetting()).thenReturn(maximum);
        LocalDate date = LocalDate.now().plusDays(2);
        Reservation overnight = reservation(date.minusDays(1), 8, LocalTime.of(23, 30), 120);
        when(reservations.findByReservationDateAndReservationStatusIn(eq(date.minusDays(1)), any()))
                .thenReturn(List.of(overnight));
        RestaurantCapacityService service = new RestaurantCapacityService(reservations, settings);

        assertThrows(ResponseStatusException.class,
                () -> service.requireCapacity(date, LocalTime.of(0, 30), 60, 4));
    }

    private Reservation reservation(LocalDate date, int guests, LocalTime time, int duration) {
        Reservation reservation = new Reservation();
        reservation.setReservationDate(date);
        reservation.setGuestCount(guests);
        reservation.setArrivalTime(time);
        reservation.setExpectedDurationMinutes(duration);
        return reservation;
    }
}
