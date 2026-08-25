package poly.edu.quanlynhahang.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.TimekeepingCheckRequest;
import poly.edu.quanlynhahang.dto.TimekeepingResponse;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;
import poly.edu.quanlynhahang.service.AttendancePolicyService;
@RestController
@RequestMapping("/api/timekeeping")
public class TimekeepingController {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private TimekeepingRepository timekeepingRepository;

    @Autowired
    private poly.edu.quanlynhahang.repository.WorkScheduleRepository workScheduleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AttendancePolicyService attendancePolicyService;

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Định dạng ngày không hợp lệ. Vui lòng dùng yyyy-MM-dd");
        }
    }

    private LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> getMyTimekeeping(Authentication authentication,
                                               @RequestParam String startDate,
                                               @RequestParam String endDate) {
        try {
            String username = authentication.getName();
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);
            List<Timekeeping> records = timekeepingRepository
                    .findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(username, start, end);
            return ResponseEntity.ok(records.stream().map(TimekeepingResponse::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> getAllTimekeeping(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);
            List<Timekeeping> records = timekeepingRepository.findByWorkDateBetweenOrderByWorkDateAsc(start, end);
            Date scheduleStart = Date.from(start.atStartOfDay(ZONE_ID).toInstant());
            Date scheduleEnd = Date.from(end.atStartOfDay(ZONE_ID).toInstant());
            Map<String, poly.edu.quanlynhahang.entity.WorkSchedule> schedules = workScheduleRepository
                    .findByWorkDateBetweenOrderByWorkDateAsc(scheduleStart, scheduleEnd).stream()
                    .collect(Collectors.toMap(this::scheduleKey, Function.identity(), (first, ignored) -> first));
            return ResponseEntity.ok(records.stream()
                    .map(record -> TimekeepingResponse.from(record,
                            schedules.get(record.getAccount().getUsername() + "|" + record.getWorkDate())))
                    .toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> getTodayStatus(Authentication authentication) {
        String username = authentication.getName();
        LocalDate today = today();
        Optional<Timekeeping> tkOpt = timekeepingRepository.findByAccountUsernameAndWorkDate(username, today);
        if (tkOpt.isPresent()) {
            return ResponseEntity.ok(TimekeepingResponse.from(tkOpt.get()));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    @Transactional
    public ResponseEntity<?> performCheck(Authentication authentication,
                                          @Valid @RequestBody TimekeepingCheckRequest payload) {
        String username = authentication.getName();
        String type = payload.type();

        // Lock the employee row so two concurrent check-in requests for the same
        // person cannot both observe an absent attendance record.
        Optional<Account> accOpt = accountRepository.findLockedByUsername(username);
        if (!accOpt.isPresent()) return ResponseEntity.badRequest().body("Không tìm thấy tài khoản");

        try {
            LocalDate today = today();
            Optional<Timekeeping> tkOpt = timekeepingRepository.findByAccountUsernameAndWorkDate(username, today);
            Timekeeping tk;

            if (type.equals("IN")) {
                if (tkOpt.isPresent() && tkOpt.get().getCheckInTime() != null) {
                    return ResponseEntity.badRequest().body("Bạn đã check-in hôm nay rồi!");
                }
                tk = tkOpt.orElse(new Timekeeping());
                tk.setAccount(accOpt.get());
                tk.setWorkDate(today);
                LocalTime now = LocalTime.now(ZONE_ID);
                tk.setCheckInTime(now);
                tk.setTotalHours(java.math.BigDecimal.ZERO);

                // Xác định đi trễ dựa trên lịch làm việc
                List<poly.edu.quanlynhahang.entity.WorkSchedule> schedules =
                        workScheduleRepository.findByAccountUsernameAndWorkDate(username,
                                Date.from(today.atStartOfDay(ZONE_ID).toInstant()));
                String status = "Đúng giờ";
                if (!schedules.isEmpty()) {
                    if (attendancePolicyService.isLate(now, schedules)) status = "Đi trễ";
                }
                tk.setStatus(status);
            } else if (type.equals("OUT")) {
                if (!tkOpt.isPresent() || tkOpt.get().getCheckInTime() == null) {
                    return ResponseEntity.badRequest().body("Bạn chưa check-in hôm nay!");
                }
                tk = tkOpt.get();
                if (tk.getCheckOutTime() != null) {
                    return ResponseEntity.badRequest().body("Bạn đã check-out hôm nay rồi!");
                }
                tk.setCheckOutTime(LocalTime.now(ZONE_ID));
                tk.setTotalHours(attendancePolicyService.totalHours(tk.getCheckInTime(), tk.getCheckOutTime()));
                // P2: Giữ nguyên trạng thái "Đi trễ" nếu đã được set, không ghi đè thành "Hoàn thành"
                if (tk.getStatus() == null || "Đúng giờ".equals(tk.getStatus())) {
                    tk.setStatus("Đúng giờ");
                }
                // Nếu đã là "Đi trễ" thì giữ nguyên
            } else {
                return ResponseEntity.badRequest().body("Loại check-in/out không hợp lệ!");
            }

            return ResponseEntity.ok(TimekeepingResponse.from(timekeepingRepository.save(tk)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String scheduleKey(poly.edu.quanlynhahang.entity.WorkSchedule schedule) {
        LocalDate date = java.time.Instant.ofEpochMilli(schedule.getWorkDate().getTime())
                .atZone(ZONE_ID).toLocalDate();
        return schedule.getAccount().getUsername() + "|" + date;
    }
}
