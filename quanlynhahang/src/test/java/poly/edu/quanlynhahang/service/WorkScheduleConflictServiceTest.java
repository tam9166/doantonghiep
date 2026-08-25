package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.WorkShiftDefinition;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;

class WorkScheduleConflictServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void rejectsDuplicateOverlappingShift() {
        WorkScheduleRepository repository = mock(WorkScheduleRepository.class);
        LocalDate date = LocalDate.of(2026, 8, 26);
        WorkSchedule morning = schedule(1L, date, WorkShiftDefinition.MORNING);
        when(repository.findByAccountUsernameAndWorkDateBetween(eq("waiter"), any(), any()))
                .thenReturn(List.of(morning));

        WorkScheduleConflictService service = new WorkScheduleConflictService(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.requireAvailable("waiter", date, WorkShiftDefinition.MORNING, null));
    }

    @Test
    void ignoresEditedScheduleItself() {
        WorkScheduleRepository repository = mock(WorkScheduleRepository.class);
        LocalDate date = LocalDate.of(2026, 8, 26);
        WorkSchedule morning = schedule(9L, date, WorkShiftDefinition.MORNING);
        when(repository.findByAccountUsernameAndWorkDateBetween(eq("waiter"), any(), any()))
                .thenReturn(List.of(morning));

        WorkScheduleConflictService service = new WorkScheduleConflictService(repository);

        assertDoesNotThrow(() -> service.requireAvailable(
                "waiter", date, WorkShiftDefinition.MORNING, 9L));
    }

    private WorkSchedule schedule(Long id, LocalDate date, WorkShiftDefinition shift) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(id);
        schedule.setWorkDate(Date.from(date.atStartOfDay(ZONE).toInstant()));
        schedule.applyShift(shift);
        return schedule;
    }
}
