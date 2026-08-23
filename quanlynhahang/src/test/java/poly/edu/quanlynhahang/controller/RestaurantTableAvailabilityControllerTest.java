package poly.edu.quanlynhahang.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import poly.edu.quanlynhahang.dto.AvailableTableResponse;
import poly.edu.quanlynhahang.service.ReservationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestaurantTableAvailabilityControllerTest {
    @Test
    void legacyCheckAvailabilityDelegatesToTheCanonicalReservationAvailabilityFlow() {
        ReservationService reservationService = mock(ReservationService.class);
        RestaurantTableController controller = new RestaurantTableController();
        ReflectionTestUtils.setField(controller, "reservationService", reservationService);
        List<AvailableTableResponse> expected = List.of();
        when(reservationService.findAvailableTables("2026-09-20", "18:00", 120, 4, 2, true))
                .thenReturn(expected);

        var response = controller.checkAvailability("2026-09-20", "18:00", 120, 4, 2, true);

        assertSame(expected, response.getBody());
        verify(reservationService).findAvailableTables("2026-09-20", "18:00", 120, 4, 2, true);
    }
}
