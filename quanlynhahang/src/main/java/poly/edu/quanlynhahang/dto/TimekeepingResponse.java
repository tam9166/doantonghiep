package poly.edu.quanlynhahang.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.entity.WorkSchedule;

public record TimekeepingResponse(Long id, EmployeeSummaryResponse employee, LocalDate workDate,
        LocalTime checkInTime, LocalTime checkOutTime, BigDecimal totalHours, String status,
        String shift, Long lateMinutes, Long earlyMinutes) {
    public static TimekeepingResponse from(Timekeeping value) {
        return new TimekeepingResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getWorkDate(), value.getCheckInTime(), value.getCheckOutTime(), value.getTotalHours(),
                value.getStatus(), null, 0L, 0L);
    }

    public static TimekeepingResponse from(Timekeeping value, WorkSchedule schedule) {
        if (schedule == null) return from(value);
        long late = value.getCheckInTime() != null && value.getCheckInTime().isAfter(schedule.getStartTime())
                ? java.time.Duration.between(schedule.getStartTime(), value.getCheckInTime()).toMinutes() : 0L;
        long early = value.getCheckOutTime() != null && value.getCheckOutTime().isBefore(schedule.getEndTime())
                ? java.time.Duration.between(value.getCheckOutTime(), schedule.getEndTime()).toMinutes() : 0L;
        return new TimekeepingResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getWorkDate(), value.getCheckInTime(), value.getCheckOutTime(), value.getTotalHours(),
                value.getStatus(), schedule.getShiftName(), late, early);
    }
}
