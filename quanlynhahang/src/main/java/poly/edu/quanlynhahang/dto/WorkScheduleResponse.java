package poly.edu.quanlynhahang.dto;

import java.util.Date;
import java.time.LocalTime;
import poly.edu.quanlynhahang.entity.WorkSchedule;

public record WorkScheduleResponse(Long id, EmployeeSummaryResponse employee, Date workDate, String shift,
        String shiftName, LocalTime startTime, LocalTime endTime, String status, String note) {
    public static WorkScheduleResponse from(WorkSchedule value) {
        return new WorkScheduleResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getWorkDate(), value.getShift(), value.getShiftName(), value.getStartTime(), value.getEndTime(),
                value.getStatus(), value.getNote());
    }
}
