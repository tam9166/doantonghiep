package poly.edu.quanlynhahang.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class WorkShiftDefinitionTest {
    @Test
    void scheduleSnapshotPopulatesEveryRequiredDatabaseField() {
        WorkSchedule schedule = new WorkSchedule();

        schedule.applyShift(WorkShiftDefinition.fromLabel("Tối"));

        assertEquals("Tối", schedule.getShift());
        assertEquals("Tối", schedule.getShiftName());
        assertEquals(LocalTime.of(22, 0), schedule.getStartTime());
        assertEquals(LocalTime.of(6, 0), schedule.getEndTime());
        assertEquals("SCHEDULED", schedule.getStatus());
    }
}
