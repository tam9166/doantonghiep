package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;
import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminAccountController {

    @Autowired private AccountRepository accountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthorityRepository authorityRepository;
    @Autowired private poly.edu.quanlynhahang.repository.OrderRepository orderRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private TimekeepingRepository timekeepingRepository;

    // 1. Lấy danh sách nhân viên
    @GetMapping
    public ResponseEntity<?> getAllStaff() {
        List<java.util.Map<String, Object>> result = accountRepository.findAll().stream()
            .filter(acc -> {
                List<Authority> auths = acc.getAuthorities();
                if (auths == null || auths.isEmpty()) {
                    String un = acc.getUsername().toLowerCase();
                    return un.equals("admin") || un.equals("manager") || un.equals("bep1") || un.equals("pv1");
                }
                return auths.stream().anyMatch(a -> {
                    String roleName = a.getRole().getName();
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
            String roleStr = "ROLE_USER";
            List<Authority> auths = acc.getAuthorities();
            if (auths != null && !auths.isEmpty()) {
                // Find highest role
                List<String> roles = auths.stream()
                        .map(a -> a.getRole().getName())
                        .collect(Collectors.toList());
                if (roles.contains("ADMIN") || roles.contains("ROLE_ADMIN")) {
                    roleStr = "ROLE_ADMIN";
                } else if (roles.contains("ROLE_MANAGER")) {
                    roleStr = "ROLE_MANAGER";
                } else if (roles.contains("ROLE_KITCHEN")) {
                    roleStr = "ROLE_KITCHEN";
                } else if (roles.contains("ROLE_WAITER")) {
                    roleStr = "ROLE_WAITER";
                } else if (roles.contains("ROLE_CASHIER")) {
                    roleStr = "ROLE_CASHIER";
                } else {
                    roleStr = roles.get(0);
                }
            }

            // BÙA HỘ MỆNH cho UI
            String username = acc.getUsername().toLowerCase();
            if (username.equals("admin") || username.equals("manager")) {
                roleStr = "ROLE_ADMIN";
            } else if (username.equals("bep1")) {
                roleStr = "ROLE_KITCHEN";
            } else if (username.equals("pv1")) {
                roleStr = "ROLE_WAITER";
            }

            map.put("role", roleStr);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 2. Thêm nhân viên mới và cấp quyền
    @PostMapping
    public ResponseEntity<?> createStaff(@RequestBody Account staffReq, @RequestParam String roleId) {
        if (accountRepository.existsById(staffReq.getUsername())) {
            return ResponseEntity.badRequest().body("Tên đăng nhập (Username) đã tồn tại!");
        }

        Account newAccount = accountRepository.save(staffReq);

        // 🛑 ĐÃ SỬA: Dùng findByName(roleId) thay vì findById
        roleRepository.findByName(roleId).ifPresent(role -> {
            Authority authority = new Authority();
            authority.setAccount(newAccount);
            authority.setRole(role);
            authorityRepository.save(authority);
        });

        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }

    // 3. Xóa / Sa thải nhân viên
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteStaff(@PathVariable String username) {
        if (accountRepository.existsById(username)) {
            
            // 🛑 ĐÃ SỬA LỖI DÒNG 45: Chống NullPointerException
            List<Authority> auths = authorityRepository.findAll().stream()
                .filter(a -> a.getAccount() != null && username.equals(a.getAccount().getUsername()))
                .collect(Collectors.toList());
                
            if (!auths.isEmpty()) {
                authorityRepository.deleteAll(auths);
            }
            
            accountRepository.deleteById(username);
            return ResponseEntity.ok("Đã xóa tài khoản nhân viên!");
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nhân viên!");
    }

    // 4. Cập nhật thông tin nhân viên
    @org.springframework.web.bind.annotation.PutMapping("/{username}")
    public ResponseEntity<?> updateStaff(@PathVariable String username, @RequestBody Account staffReq, @RequestParam(required = false) String roleId) {
        return accountRepository.findById(username).map(existing -> {
            if (staffReq.getFullname() != null) existing.setFullname(staffReq.getFullname());
            if (staffReq.getEmail() != null) existing.setEmail(staffReq.getEmail());
            if (staffReq.getPassword() != null && !staffReq.getPassword().isEmpty()) {
                existing.setPassword(staffReq.getPassword());
            }
            
            accountRepository.save(existing);
            
            // Cập nhật role nếu có
            if (roleId != null && !roleId.isEmpty()) {
                List<Authority> auths = authorityRepository.findAll().stream()
                    .filter(a -> a.getAccount() != null && username.equals(a.getAccount().getUsername()))
                    .collect(Collectors.toList());
                authorityRepository.deleteAll(auths);
                
                roleRepository.findByName(roleId).ifPresent(role -> {
                    Authority newAuth = new Authority();
                    newAuth.setAccount(existing);
                    newAuth.setRole(role);
                    authorityRepository.save(newAuth);
                });
            }
            return ResponseEntity.ok("Cập nhật tài khoản thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy tài khoản nhân viên!"));
    }
    // 5. Lấy danh sách khách hàng
    @GetMapping("/customers")
    public ResponseEntity<?> getAllCustomers() {
        List<java.util.Map<String, Object>> result = accountRepository.findAll().stream()
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
                map.put("points", acc.getPoints());
                map.put("membershipTier", acc.getMembershipTier());
                return map;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 6. Xem lịch sử hóa đơn của khách hàng
    @GetMapping("/customers/{username}/orders")
    public ResponseEntity<?> getCustomerOrders(@PathVariable String username) {
        return ResponseEntity.ok(orderRepository.findByAccountUsername(username));
    }
}