package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.dto.JwtResponse;
import poly.edu.quanlynhahang.dto.LoginRequest;
import poly.edu.quanlynhahang.dto.SignupRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;
import poly.edu.quanlynhahang.security.JwtUtils;

@CrossOrigin(origins = "*", maxAge = 3600)
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

    // API 1: ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);
        
        List<String> roles = authentication.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt, loginRequest.getUsername(), roles));
    }

    // API 2: ĐĂNG KÝ TÀI KHOẢN MỚI
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        
        if (accountRepository.existsById(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Lỗi: Tên đăng nhập đã được sử dụng!");
        }

        Account account = new Account();
        account.setUsername(signUpRequest.getUsername());
        account.setPassword(signUpRequest.getPassword()); 
        account.setFullname(signUpRequest.getFullname());
        account.setEmail(signUpRequest.getEmail());
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

    // API 4: CỘNG ĐIỂM TỪ VÒNG QUAY MAY MẮN
    @PostMapping("/add-points")
    public ResponseEntity<?> addPoints(@RequestBody java.util.Map<String, Integer> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Integer pointsToAdd = payload.get("points");
        if (pointsToAdd == null || pointsToAdd <= 0) {
            return ResponseEntity.badRequest().body("Số điểm không hợp lệ");
        }

        java.util.Optional<Account> accOpt = accountRepository.findById(username);
        if (accOpt.isPresent()) {
            Account acc = accOpt.get();
            int currentPoints = acc.getPoints() != null ? acc.getPoints() : 0;
            int newPoints = currentPoints + pointsToAdd;
            acc.setPoints(newPoints);

            String newTier = "Đồng";
            if (newPoints >= 2000) newTier = "Kim Cương";
            else if (newPoints >= 1000) newTier = "Vàng";
            else if (newPoints >= 500) newTier = "Bạc";
            
            acc.setMembershipTier(newTier);
            accountRepository.save(acc);

            return ResponseEntity.ok(java.util.Map.of(
                "message", "Cộng điểm thành công",
                "newPoints", newPoints,
                "newTier", newTier
            ));
        }
        return ResponseEntity.badRequest().body("Lỗi: Không tìm thấy tài khoản");
    }
}