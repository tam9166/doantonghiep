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

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.TimekeepingCheckRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;
@RestController
@RequestMapping("/api/timekeeping")
public class TimekeepingController {

    @Autowired
    private TimekeepingRepository timekeepingRepository;

    @Autowired
    private poly.edu.quanlynhahang.repository.WorkScheduleRepository workScheduleRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Xem lịch sử chấm công của mình
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> getMyTimekeeping(Authentication authentication, @RequestParam String startDate, @RequestParam String endDate) {
        try {
            String username = authentication.getName();
            Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
            Date end = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
            List<Timekeeping> records = timekeepingRepository.findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(username, start, end);
            return ResponseEntity.ok(records);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ.");
        }
    }

    // Admin xem tất cả
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> getAllTimekeeping(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            Date start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
            Date end = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
            List<Timekeeping> records = timekeepingRepository.findByWorkDateBetweenOrderByWorkDateAsc(start, end);
            return ResponseEntity.ok(records);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ.");
        }
    }

    // Lấy trạng thái chấm công của ngày hôm nay
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> getTodayStatus(Authentication authentication) {
        try {
            String username = authentication.getName();
            String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Date today = new SimpleDateFormat("yyyy-MM-dd").parse(todayStr);
            
            Optional<Timekeeping> tkOpt = timekeepingRepository.findByAccountUsernameAndWorkDate(username, today);
            if (tkOpt.isPresent()) {
                return ResponseEntity.ok(tkOpt.get());
            }
            return ResponseEntity.ok().build(); // chưa có bản ghi
        } catch (ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Thực hiện Check-in / Check-out
    @PostMapping("/check")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
    public ResponseEntity<?> performCheck(Authentication authentication, @Valid @RequestBody TimekeepingCheckRequest payload) {
        String username = authentication.getName();
        String type = payload.type();

        Optional<Account> accOpt = accountRepository.findById(username);
        if (!accOpt.isPresent()) return ResponseEntity.badRequest().body("Không tìm thấy tài khoản");

        try {
            String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Date today = new SimpleDateFormat("yyyy-MM-dd").parse(todayStr);
            
            Optional<Timekeeping> tkOpt = timekeepingRepository.findByAccountUsernameAndWorkDate(username, today);
            Timekeeping tk;

            if (type.equals("IN")) {
                if (tkOpt.isPresent() && tkOpt.get().getCheckInTime() != null) {
                    return ResponseEntity.badRequest().body("Bạn đã check-in hôm nay rồi!");
                }
                tk = tkOpt.orElse(new Timekeeping());
                tk.setAccount(accOpt.get());
                tk.setWorkDate(today);
                Date now = new Date();
                tk.setCheckInTime(now);
                
                // Xác định đi trễ
                List<poly.edu.quanlynhahang.entity.WorkSchedule> schedules = workScheduleRepository.findByAccountUsernameAndWorkDate(username, today);
                String status = "Đúng giờ";
                if (!schedules.isEmpty()) {
                    poly.edu.quanlynhahang.entity.WorkSchedule schedule = schedules.get(0);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(now);
                    int currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
                    
                    if ("Sáng".equals(schedule.getShift()) && currentMinutes > (6 * 60 + 15)) {
                        status = "Đi trễ";
                    } else if ("Chiều".equals(schedule.getShift()) && currentMinutes > (14 * 60 + 15)) {
                        status = "Đi trễ";
                    } else if ("Tối".equals(schedule.getShift()) && currentMinutes > (22 * 60 + 15)) {
                        status = "Đi trễ";
                    } else if (schedule.getShift() != null && schedule.getShift().matches(".*\\d{2}:\\d{2}.*")) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{2}):(\\d{2})").matcher(schedule.getShift());
                        if (m.find()) {
                            int expectedHour = Integer.parseInt(m.group(1));
                            int expectedMin = Integer.parseInt(m.group(2));
                            if (currentMinutes > (expectedHour * 60 + expectedMin + 15)) {
                                status = "Đi trễ";
                            }
                        }
                    }
                }
                tk.setStatus(status);
            } else if (type.equals("OUT")) {
                if (!tkOpt.isPresent() || tkOpt.get().getCheckInTime() == null) {
                    return ResponseEntity.badRequest().body("Bạn phải check-in trước khi check-out!");
                }
                tk = tkOpt.get();
                tk.setCheckOutTime(new Date());
                tk.setStatus("Hoàn thành");
            } else {
                return ResponseEntity.badRequest().body("Loại chấm công không hợp lệ.");
            }

            return ResponseEntity.ok(timekeepingRepository.save(tk));
        } catch (ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
