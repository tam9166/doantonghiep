package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import poly.edu.quanlynhahang.dto.JwtResponse;
import poly.edu.quanlynhahang.dto.LoginRequest;
import poly.edu.quanlynhahang.dto.SignupRequest;
import poly.edu.quanlynhahang.dto.UpdateProfileRequest;
import poly.edu.quanlynhahang.dto.ChangePasswordRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;
import poly.edu.quanlynhahang.security.JwtUtils;
import poly.edu.quanlynhahang.security.PasswordPolicy;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Danh sách các role nhân sự (dùng chung cho cả 2 endpoint)
    private static final List<String> STAFF_ROLES = List.of(
        "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER"
    );

    // API 1: ĐĂNG NHẬP KHÁCH HÀNG (Chặn nhân sự)
    @PostMapping("/login")
    public ResponseEntity<?> customerLogin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String username = normalizeUsername(loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);
            
            List<String> roles = authentication.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .filter(role -> role.startsWith("ROLE_"))
                    .distinct()
                    .collect(Collectors.toList());

            // 🛡️ CHẶN: Nếu tài khoản có role nhân sự → không cho đăng nhập ở cổng khách hàng
            if (roles.stream().anyMatch(STAFF_ROLES::contains)) {
                SecurityContextHolder.clearContext();
                return ResponseEntity.status(403)
                    .body("Vui lòng sử dụng trang đăng nhập dành cho nhân viên.");
            }

            // Lấy thông tin phụ của user
            Account acc = accountRepository.findById(username).orElse(new Account());

            return ResponseEntity.ok(new JwtResponse(jwt, username, roles,
                    acc.getAssignedArea(), acc.getShift(), Boolean.TRUE.equals(acc.getMustChangePassword())));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu.");
        }
    }

    // API 1B: ĐĂNG NHẬP NHÂN SỰ / ADMIN (Chặn khách hàng)
    @PostMapping("/staff/login")
    public ResponseEntity<?> staffLogin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String username = normalizeUsername(loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);
            
            List<String> roles = authentication.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .filter(role -> role.startsWith("ROLE_"))
                    .distinct()
                    .collect(Collectors.toList());

            // 🛡️ CHẶN: Nếu tài khoản KHÔNG có role nhân sự → không cho đăng nhập ở cổng quản trị
            if (roles.stream().noneMatch(STAFF_ROLES::contains)) {
                SecurityContextHolder.clearContext();
                return ResponseEntity.status(403)
                    .body("Tài khoản không có quyền truy cập hệ thống quản trị.");
            }

            // Lấy thông tin phụ của user
            Account acc = accountRepository.findById(username).orElse(new Account());

            return ResponseEntity.ok(new JwtResponse(jwt, username, roles,
                    acc.getAssignedArea(), acc.getShift(), Boolean.TRUE.equals(acc.getMustChangePassword())));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu nội bộ.");
        }
    }

    // API 2: ĐĂNG KÝ TÀI KHOẢN MỚI
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        String username = normalizeUsername(signUpRequest.getUsername());
        PasswordPolicy.validate(signUpRequest.getPassword());

        if (accountRepository.existsById(username)) {
            return ResponseEntity.badRequest().body("Lỗi: Tên đăng nhập đã được sử dụng!");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        account.setFullname(signUpRequest.getFullname().trim());
        account.setEmail(signUpRequest.getEmail().trim().toLowerCase(Locale.ROOT));
        accountRepository.save(account);

        Role userRole = roleRepository.findByName("USER").orElse(null);
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        Authority authority = new Authority();
        authority.setAccount(account);
        authority.setRole(userRole);
        authorityRepository.save(authority);

        return ResponseEntity.ok("Đăng ký tài khoản thành công!");
    }

    // API 3: LẤY THÔNG TIN PROFILE (Tích điểm & Hạng thành viên)
    @org.springframework.web.bind.annotation.GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findById(username)
                .map(acc -> ResponseEntity.ok(java.util.Map.of(
                    "username", acc.getUsername(),
                    "fullname", acc.getFullname(),
                    "email", acc.getEmail(),
                    "points", acc.getPoints() != null ? acc.getPoints() : 0,
                    "membershipTier", acc.getMembershipTier() != null ? acc.getMembershipTier() : "Đồng"
                )))
                .orElse(ResponseEntity.badRequest().build());
    }

    // API 4: CẬP NHẬT THÔNG TIN CÁ NHÂN
    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        java.util.Optional<Account> accOpt = accountRepository.findById(username);
        if (accOpt.isPresent()) {
            Account acc = accOpt.get();
            acc.setFullname(updateRequest.getFullname().trim());
            acc.setEmail(updateRequest.getEmail().trim().toLowerCase(Locale.ROOT));
            accountRepository.save(acc);
            return ResponseEntity.ok("Cập nhật thông tin thành công!");
        }
        return ResponseEntity.badRequest().body("Lỗi: Không tìm thấy tài khoản");
    }

    // API 5: ĐỔI MẬT KHẨU
    @org.springframework.web.bind.annotation.PutMapping("/password")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        java.util.Optional<Account> accOpt = accountRepository.findLockedByUsername(username);
        if (accOpt.isPresent()) {
            Account acc = accOpt.get();
            if (!passwordEncoder.matches(request.getOldPassword(), acc.getPassword())) {
                return ResponseEntity.badRequest().body("Mật khẩu cũ không chính xác!");
            }
            PasswordPolicy.validate(request.getNewPassword());
            acc.setPassword(passwordEncoder.encode(request.getNewPassword()));
            acc.setMustChangePassword(false);
            acc.setTokenVersion((acc.getTokenVersion() == null ? 0L : acc.getTokenVersion()) + 1L);
            accountRepository.save(acc);
            return ResponseEntity.ok("Đổi mật khẩu thành công!");
        }
        return ResponseEntity.badRequest().body("Lỗi: Không tìm thấy tài khoản");
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
