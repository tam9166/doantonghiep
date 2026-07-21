package poly.edu.quanlynhahang.dto;

import java.util.Date;
import poly.edu.quanlynhahang.entity.Timekeeping;

public record TimekeepingResponse(Long id, EmployeeSummaryResponse employee, Date workDate,
        Date checkInTime, Date checkOutTime, String status) {
    public static TimekeepingResponse from(Timekeeping value) {
        return new TimekeepingResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getWorkDate(), value.getCheckInTime(), value.getCheckOutTime(), value.getStatus());
    }
}
