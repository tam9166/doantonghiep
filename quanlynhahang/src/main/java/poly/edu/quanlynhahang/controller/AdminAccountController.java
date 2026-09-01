package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.dto.CreateStaffRequest;
import poly.edu.quanlynhahang.dto.UpdateStaffRequest;
import poly.edu.quanlynhahang.dto.UpdateCustomerRequest;
import poly.edu.quanlynhahang.dto.OrderResponse;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.service.StaffAccountService;

@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminAccountController {

    @Autowired private AccountRepository accountRepository;
    @Autowired private poly.edu.quanlynhahang.repository.OrderRepository orderRepository;
    @Autowired private StaffAccountService staffAccountService;

    // 1. Lấy danh sách nhân viên
    @GetMapping
    public ResponseEntity<?> getAllStaff() {
        List<java.util.Map<String, Object>> result = accountRepository.findAllWithAuthorities().stream()
            .filter(acc -> !Boolean.FALSE.equals(acc.getEnabled()))
            .filter(acc -> {
                List<Authority> auths = acc.getAuthorities();
                if (auths == null || auths.isEmpty()) return false;
                return auths.stream().anyMatch(a -> {
                    String roleName = a.getRole().getName().toUpperCase();
                    return roleName.contains("ADMIN") || roleName.contains("MANAGER") || 
                           roleName.contains("KITCHEN") || roleName.contains("WAITER") || 
                           roleName.contains("CASHIER");
                });
            })
            .map(acc -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", acc.getUsername());
            map.put("fullname", acc.getFullname());
            map.put("email", acc.getEmail());
            map.put("shift", acc.getShift());
            map.put("assignedArea", acc.getAssignedArea());
            map.put("shiftRate", acc.getShiftRate());
            String roleStr = "ROLE_USER";
            List<Authority> auths = acc.getAuthorities();
            if (auths != null && !auths.isEmpty()) {
                // Find highest role
                List<String> roles = auths.stream()
                        .map(a -> a.getRole().getName())
                        .collect(Collectors.toList());
                if (roles.contains("ADMIN") || roles.contains("ROLE_ADMIN")) {
                    roleStr = "ROLE_ADMIN";
                } else if (roles.contains("MANAGER") || roles.contains("ROLE_MANAGER")) {
                    roleStr = "ROLE_MANAGER";
                } else if (roles.contains("KITCHEN") || roles.contains("ROLE_KITCHEN")) {
                    roleStr = "ROLE_KITCHEN";
                } else if (roles.contains("WAITER") || roles.contains("ROLE_WAITER")) {
                    roleStr = "ROLE_WAITER";
                } else if (roles.contains("CASHIER") || roles.contains("ROLE_CASHIER")) {
                    roleStr = "ROLE_CASHIER";
                } else {
                    roleStr = roles.get(0);
                }
            }

            map.put("role", roleStr);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 2. Thêm nhân viên mới và cấp quyền
    @PostMapping
    public ResponseEntity<?> createStaff(@Valid @RequestBody CreateStaffRequest staffReq,
                                         @RequestParam String roleId) {
        staffAccountService.create(staffReq, roleId);
        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }

    // 3. Xóa / Sa thải nhân viên
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteStaff(@PathVariable String username) {
        staffAccountService.disable(username);
        return ResponseEntity.ok("Đã khóa tài khoản nhân viên!");
    }

    // 4. Cập nhật thông tin nhân viên
    @org.springframework.web.bind.annotation.PutMapping("/{username}")
    public ResponseEntity<?> updateStaff(@PathVariable String username,
                                         @Valid @RequestBody UpdateStaffRequest staffReq,
                                         @RequestParam(required = false) String roleId) {
        staffAccountService.update(username, staffReq, roleId);
        return ResponseEntity.ok("Cập nhật tài khoản thành công!");
    }
    // 5. Lấy danh sách khách hàng
    @GetMapping("/customers")
    public ResponseEntity<?> getAllCustomers() {
        List<java.util.Map<String, Object>> result = accountRepository.findAllWithAuthorities().stream()
            .filter(acc -> {
                List<Authority> auths = acc.getAuthorities();
                if (auths == null || auths.isEmpty()) {
                    String un = acc.getUsername().toLowerCase();
                    return !un.equals("admin") && !un.equals("manager") && !un.equals("bep1") && !un.equals("pv1");
                }
                // Khách hàng là người CHỈ có ROLE_USER hoặc không có role nào khác ngoài ROLE_USER
                return auths.stream().allMatch(a -> a.getRole().getName().equals("ROLE_USER") || a.getRole().getName().equals("USER"));
            })
            .map(acc -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("username", acc.getUsername());
                map.put("fullname", acc.getFullname());
                map.put("email", acc.getEmail());
                map.put("phone", acc.getPhone());
                // Expose an intentional status label instead of the internal
                // account flag, which must remain hidden from normal API DTOs.
                map.put("status", Boolean.FALSE.equals(acc.getEnabled()) ? "LOCKED" : "ACTIVE");
                map.put("points", acc.getPoints());
                map.put("membershipTier", acc.getMembershipTier());
                return map;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 6. Xem lịch sử hóa đơn của khách hàng
    @GetMapping("/customers/{username}/orders")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerOrders(@PathVariable String username) {
        return ResponseEntity.ok(orderRepository.findByAccountUsernameWithDetails(username).stream()
                .map(OrderResponse::from).toList());
    }

    @PutMapping("/customers/{username}")
    public ResponseEntity<?> updateCustomer(@PathVariable String username,
                                            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(staffAccountService.updateCustomer(username, request));
    }

    @PostMapping("/customers/{username}/reset-password")
    public ResponseEntity<?> resetCustomerPassword(@PathVariable String username,
                                                   @RequestBody java.util.Map<String, String> request) {
        String password = request == null ? null : request.get("password");
        staffAccountService.resetCustomerPassword(username, password);
        return ResponseEntity.ok(java.util.Map.of("message", "Đã đặt lại mật khẩu; khách cần đổi mật khẩu khi đăng nhập."));
    }

    @DeleteMapping("/customers/{username}")
    public ResponseEntity<?> disableCustomer(@PathVariable String username) {
        staffAccountService.disableCustomer(username);
        return ResponseEntity.ok("Đã khóa tài khoản khách hàng.");
    }

    @PutMapping("/customers/{username}/status")
    public ResponseEntity<?> setCustomerStatus(@PathVariable String username,
                                                @RequestParam boolean enabled) {
        staffAccountService.setCustomerEnabled(username, enabled);
        return ResponseEntity.ok(enabled ? "Đã mở khóa tài khoản khách hàng." : "Đã khóa tài khoản khách hàng.");
    }
}
