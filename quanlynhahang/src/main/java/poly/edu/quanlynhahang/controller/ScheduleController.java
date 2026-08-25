package poly.edu.quanlynhahang.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.WorkShiftDefinition;
import poly.edu.quanlynhahang.dto.WorkScheduleResponse;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;
import poly.edu.quanlynhahang.service.WorkScheduleConflictService;
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private WorkScheduleRepository workScheduleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WorkScheduleConflictService conflictService;

    // Lấy lịch làm việc theo khoảng thời gian (dành cho Admin xem tất cả)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> getSchedules(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            Date start = parseBusinessDate(startDate);
            Date end = parseBusinessDate(endDate);
            List<WorkSchedule> schedules = workScheduleRepository.findByWorkDateBetweenOrderByWorkDateAsc(start, end);
            return ResponseEntity.ok(schedules.stream().map(WorkScheduleResponse::from).toList());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ. Vui lòng dùng yyyy-MM-dd");
        }
    }

    // Nhân viên xem lịch của chính mình
    @GetMapping("/my-schedules")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> getMySchedules(Authentication authentication, @RequestParam String startDate, @RequestParam String endDate) {
        try {
            String username = authentication.getName();
            Date start = parseBusinessDate(startDate);
            Date end = parseBusinessDate(endDate);
            List<WorkSchedule> schedules = workScheduleRepository.findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(username, start, end);
            return ResponseEntity.ok(schedules.stream().map(WorkScheduleResponse::from).toList());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ.");
        }
    }

    // Admin tạo/cập nhật lịch làm
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> createSchedule(@Valid @RequestBody poly.edu.quanlynhahang.dto.WorkScheduleRequest request) {
        Optional<Account> accOpt = accountRepository.findById(request.getUsername());
        if (!accOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Tài khoản không tồn tại!");
        }

        try {
            LocalDate businessDate = LocalDate.parse(request.getWorkDate());
            Date workDate = parseBusinessDate(request.getWorkDate());
            WorkShiftDefinition shift = WorkShiftDefinition.fromLabel(request.getShift());
            conflictService.requireAvailable(request.getUsername(), businessDate, shift, null);

            WorkSchedule ws = new WorkSchedule();
            ws.setAccount(accOpt.get());
            ws.setWorkDate(workDate);
            ws.applyShift(shift);
            
            return ResponseEntity.ok(WorkScheduleResponse.from(workScheduleRepository.save(ws)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
                                             @Valid @RequestBody poly.edu.quanlynhahang.dto.WorkScheduleRequest request) {
        try {
            WorkSchedule schedule = workScheduleRepository.findById(id).orElse(null);
            if (schedule == null) return ResponseEntity.notFound().build();
            Account account = accountRepository.findById(request.getUsername()).orElse(null);
            if (account == null) return ResponseEntity.badRequest().body("Tài khoản không tồn tại!");
            LocalDate businessDate = LocalDate.parse(request.getWorkDate());
            WorkShiftDefinition shift = WorkShiftDefinition.fromLabel(request.getShift());
            conflictService.requireAvailable(request.getUsername(), businessDate, shift, id);
            schedule.setAccount(account);
            schedule.setWorkDate(parseBusinessDate(request.getWorkDate()));
            schedule.applyShift(shift);
            return ResponseEntity.ok(WorkScheduleResponse.from(workScheduleRepository.save(schedule)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin xóa lịch làm
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        if (workScheduleRepository.existsById(id)) {
            workScheduleRepository.deleteById(id);
            return ResponseEntity.ok("Đã xóa lịch làm!");
        }
        return ResponseEntity.notFound().build();
    }

    private Date parseBusinessDate(String value) {
        return Date.from(LocalDate.parse(value).atStartOfDay(BUSINESS_ZONE).toInstant());
    }
}
