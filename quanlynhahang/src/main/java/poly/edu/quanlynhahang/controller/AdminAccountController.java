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

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminAccountController {

    @Autowired private AccountRepository accountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthorityRepository authorityRepository;

    // 1. Lấy danh sách nhân viên
    @GetMapping
    public ResponseEntity<?> getAllStaff() {
        List<Account> accounts = accountRepository.findAll();
        return ResponseEntity.ok(accounts);
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
}