package poly.edu.quanlynhahang.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.service.PayrollService;

@RestController
@RequestMapping("/api/admin/payroll")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
public class PayrollController {
    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping
    public ResponseEntity<?> getPayroll(@RequestParam String month) {
        try {
            return ResponseEntity.ok(payrollService.calculate(YearMonth.parse(month)));
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().body("Tháng không hợp lệ. Vui lòng dùng yyyy-MM");
        }
    }
}
