package poly.edu.quanlynhahang.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import poly.edu.quanlynhahang.entity.Timekeeping;

public record TimekeepingResponse(Long id, EmployeeSummaryResponse employee, LocalDate workDate,
        LocalTime checkInTime, LocalTime checkOutTime, BigDecimal totalHours, String status) {
    public static TimekeepingResponse from(Timekeeping value) {
        return new TimekeepingResponse(value.getId(), EmployeeSummaryResponse.from(value.getAccount()),
                value.getWorkDate(), value.getCheckInTime(), value.getCheckOutTime(), value.getTotalHours(), value.getStatus());
    }
}
