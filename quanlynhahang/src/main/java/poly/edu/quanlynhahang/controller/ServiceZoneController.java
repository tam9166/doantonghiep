package poly.edu.quanlynhahang.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.ServiceZoneAssignment;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.ServiceZoneAssignmentRepository;
@RestController
@RequestMapping("/api/service-zones")
public class ServiceZoneController {

    @Autowired
    private ServiceZoneAssignmentRepository zoneRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private RestaurantTableRepository tableRepo;

    // === 1. Lấy toàn bộ phân công theo ngày (Admin xem) ===
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> getZonesByDate(@RequestParam String date) {
        try {
            Date workDate = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            List<ServiceZoneAssignment> zones = zoneRepo.findByWorkDate(workDate);
            return ResponseEntity.ok(zones);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ. Vui lòng dùng yyyy-MM-dd");
        }
    }

    // === 2. Nhân viên xem khu vực mình được phân ===
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_WAITER', 'ROLE_CASHIER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> getMyZones(Authentication authentication, @RequestParam String date) {
        try {
            String username = authentication.getName();
            Date workDate = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            List<ServiceZoneAssignment> zones = zoneRepo.findByAccountUsernameAndWorkDate(username, workDate);
            return ResponseEntity.ok(zones);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Định dạng ngày không hợp lệ.");
        }
    }

    // === 3. Admin phân công khu vực ===
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> createZoneAssignment(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String floor = request.get("floor");
        String shift = request.get("shift");
        String workDateStr = request.get("workDate");

        if (username == null || floor == null || shift == null || workDateStr == null) {
            return ResponseEntity.badRequest().body("Vui lòng điền đầy đủ thông tin!");
        }

        Optional<Account> accOpt = accountRepo.findById(username);
        if (!accOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Tài khoản không tồn tại!");
        }

        try {
            Date workDate = new SimpleDateFormat("yyyy-MM-dd").parse(workDateStr);

            // Kiểm tra trùng lặp
            List<ServiceZoneAssignment> existing = zoneRepo
                    .findByAccountUsernameAndFloorAndShiftAndWorkDate(username, floor, shift, workDate);
            if (!existing.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Nhân viên này đã được phân công tầng \"" + floor + "\" ca \"" + shift + "\" ngày này rồi!");
            }

            ServiceZoneAssignment zone = new ServiceZoneAssignment();
            zone.setAccount(accOpt.get());
            zone.setFloor(floor);
            zone.setShift(shift);
            zone.setWorkDate(workDate);

            return ResponseEntity.ok(zoneRepo.save(zone));
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Ngày không hợp lệ.");
        }
    }

    // === 4. Admin xóa phân công ===
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> deleteZoneAssignment(@PathVariable Long id) {
        if (zoneRepo.existsById(id)) {
            zoneRepo.deleteById(id);
            return ResponseEntity.ok("Đã xóa phân công khu vực!");
        }
        return ResponseEntity.notFound().build();
    }

    // === 5. Lấy danh sách tầng từ bàn thực tế ===
    @GetMapping("/floors")
    public ResponseEntity<?> getAvailableFloors() {
        List<String> floors = tableRepo.findAll().stream()
                .map(RestaurantTable::getFloor)
                .filter(f -> f != null && !f.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(floors);
    }

    // === 6. Lấy bản đồ phân công: tầng → danh sách nhân viên (theo ngày + ca) ===
    @GetMapping("/map")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> getZoneMap(@RequestParam String date, @RequestParam(required = false) String shift) {
        try {
            Date workDate = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            List<ServiceZoneAssignment> zones;

            if (shift != null && !shift.isEmpty()) {
                zones = zoneRepo.findByWorkDateAndShift(workDate, shift);
            } else {
                zones = zoneRepo.findByWorkDate(workDate);
            }

            // Group by floor → list of staff names
            Map<String, List<Map<String, String>>> map = zones.stream()
                    .collect(Collectors.groupingBy(
                            ServiceZoneAssignment::getFloor,
                            LinkedHashMap::new,
                            Collectors.mapping(z -> {
                                Map<String, String> m = new LinkedHashMap<>();
                                m.put("username", z.getAccount().getUsername());
                                m.put("fullname", z.getAccount().getFullname());
                                m.put("shift", z.getShift());
                                return m;
                            }, Collectors.toList())
                    ));

            return ResponseEntity.ok(map);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().body("Ngày không hợp lệ.");
        }
    }
}
