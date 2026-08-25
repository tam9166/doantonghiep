package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

public record PayrollRowResponse(String username, String fullname, String role,
        long scheduledShifts, long workedShifts, BigDecimal rate,
        BigDecimal totalSalary, boolean rateConfigured) {
}
