package poly.edu.quanlynhahang.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.controller.ScheduleController;
import poly.edu.quanlynhahang.controller.ServiceZoneController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorkScheduleRequestValidationTest {
    @Test
    void rejectsMissingOrUnsupportedScheduleFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            WorkScheduleRequest request = new WorkScheduleRequest();
            request.setUsername(" ");
            request.setWorkDate("20/08/2026");
            request.setShift("Đêm");

            assertFalse(validator.validate(request).isEmpty());
        }
    }

    @Test
    void scheduleAndServiceZoneRejectLenientCalendarDates() {
        assertEquals(400, new ScheduleController()
                .getSchedules("2026-02-31", "2026-03-01").getStatusCode().value());
        assertEquals(400, new ServiceZoneController()
                .getZoneMap("2026-02-31", null).getStatusCode().value());
    }
}
