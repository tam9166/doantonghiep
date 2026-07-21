package poly.edu.quanlynhahang.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.dto.WorkScheduleResponse;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private WorkScheduleRepository workScheduleRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Lấy lịch làm việc theo khoảng thời gian (dành cho Admin xem tất cả)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> getSchedules(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
            Date end = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
            List<WorkSchedule> schedules = workScheduleRepository.findByWorkDateBetweenOrderByWorkDateAsc(start, end);
            return ResponseEntity.ok(schedules.stream().map(WorkScheduleResponse::from).toList());
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ. Vui lòng dùng yyyy-MM-dd");
        }
    }

    // Nhân viên xem lịch của chính mình
    @GetMapping("/my-schedules")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> getMySchedules(Authentication authentication, @RequestParam String startDate, @RequestParam String endDate) {
        try {
            String username = authentication.getName();
            Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
            Date end = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
            List<WorkSchedule> schedules = workScheduleRepository.findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(username, start, end);
            return ResponseEntity.ok(schedules.stream().map(WorkScheduleResponse::from).toList());
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ.");
        }
    }

    // Admin tạo/cập nhật lịch làm
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> createSchedule(@RequestBody poly.edu.quanlynhahang.dto.WorkScheduleRequest request) {
        Optional<Account> accOpt = accountRepository.findById(request.getUsername());
        if (!accOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Tài khoản không tồn tại!");
        }

        try {
            Date workDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getWorkDate());
            
            // Kiểm tra xem ngày đó nhân viên đã có ca này chưa
            List<WorkSchedule> existing = workScheduleRepository.findByAccountUsernameAndWorkDate(request.getUsername(), workDate);
            for (WorkSchedule ws : existing) {
                if (ws.getShift().equals(request.getShift())) {
                    return ResponseEntity.badRequest().body("Nhân viên này đã được xếp ca " + request.getShift() + " vào ngày này rồi!");
                }
            }

            WorkSchedule ws = new WorkSchedule();
            ws.setAccount(accOpt.get());
            ws.setWorkDate(workDate);
            ws.setShift(request.getShift());
            
            return ResponseEntity.ok(WorkScheduleResponse.from(workScheduleRepository.save(ws)));
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Ngày không hợp lệ.");
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
}
