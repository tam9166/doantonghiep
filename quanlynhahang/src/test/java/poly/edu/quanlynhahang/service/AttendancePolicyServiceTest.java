package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.WorkShiftDefinition;

class AttendancePolicyServiceTest {
    private final AttendancePolicyService policy = new AttendancePolicyService(15);

    @Test
    void derivesLatenessFromPersistedScheduleStartInsteadOfShiftLabelConstants() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setShift("Ca linh hoạt");
        schedule.setStartTime(LocalTime.of(10, 30));

        assertFalse(policy.isLate(LocalTime.of(10, 45), List.of(schedule)));
        assertTrue(policy.isLate(LocalTime.of(10, 46), List.of(schedule)));
    }

    @Test
    void choosesTheClosestAssignedShiftAndHandlesOvernightHours() {
        WorkSchedule morning = new WorkSchedule();
        morning.applyShift(WorkShiftDefinition.MORNING);
        WorkSchedule night = new WorkSchedule();
        night.applyShift(WorkShiftDefinition.NIGHT);

        assertFalse(policy.isLate(LocalTime.of(21, 55), List.of(morning, night)));
        assertTrue(policy.isLate(LocalTime.of(0, 30), List.of(night)));
        assertEquals(new BigDecimal("8.00"), policy.totalHours(LocalTime.of(22, 0), LocalTime.of(6, 0)));
    }
}
