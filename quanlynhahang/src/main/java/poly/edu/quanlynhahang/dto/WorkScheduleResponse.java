package poly.edu.quanlynhahang.dto;

import java.util.Date;
import poly.edu.quanlynhahang.entity.WorkSchedule;

public record WorkScheduleResponse(Long id, EmployeeSummaryResponse employee, Date workDate, String shift) {
    public static WorkScheduleResponse from(WorkSchedule value) {
        return new WorkScheduleResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getWorkDate(), value.getShift());
    }
}
