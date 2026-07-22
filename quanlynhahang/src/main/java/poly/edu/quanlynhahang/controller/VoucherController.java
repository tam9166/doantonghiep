package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.VoucherResponse;
import poly.edu.quanlynhahang.dto.VoucherUpsertRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;
import poly.edu.quanlynhahang.service.LuckyWheelService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LuckyWheelService luckyWheelService;

    // Admin: Get all vouchers
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAllVouchers() {
        return ResponseEntity.ok(voucherRepository.findAll().stream().map(VoucherResponse::from).toList());
    }

    // User: Get my active vouchers
    @GetMapping("/my-vouchers")
    public ResponseEntity<?> getMyVouchers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Voucher> myVouchers = voucherRepository.findByAccountUsername(username);
        return ResponseEntity.ok(myVouchers.stream().map(VoucherResponse::from).toList());
    }

    // Reward selection, daily limit and eligibility are controlled entirely by the backend.
    @PostMapping("/spin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> spinLuckyWheel() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(luckyWheelService.spin(username));
    }

    // Admin: Manually create voucher
    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> adminCreateVoucher(@Valid @RequestBody VoucherUpsertRequest request) {
        Voucher voucher = new Voucher();
        if (request.code() == null || request.code().isBlank()) {
            voucher.setCode("CODE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            voucher.setCode(request.code().trim());
        }
        voucher.setDiscountPercent(request.discountPercent());
        voucher.setCreateDate(new Date());
        voucher.setIsUsed(false);
        
        // Nếu admin chỉ định user cụ thể
        if (request.account() != null) {
            Optional<Account> accOpt = accountRepository.findById(request.account().username().trim());
            if (accOpt.isPresent()) {
                voucher.setAccount(accOpt.get());
            } else {
                return ResponseEntity.unprocessableEntity().body(Map.of("code", "ACCOUNT_NOT_FOUND"));
            }
        }

        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(VoucherResponse.from(saved));
    }

    // Check voucher validity
    @PostMapping("/check")
    public ResponseEntity<?> checkVoucher(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Voucher> vOpt = voucherRepository.findByCode(code);
        if (!vOpt.isPresent()) return ResponseEntity.badRequest().body("Mã giảm giá không tồn tại!");

        Voucher voucher = vOpt.get();
        if (voucher.getIsUsed()) return ResponseEntity.badRequest().body("Mã giảm giá này đã được sử dụng!");

        // Kiểm tra xem mã này có gán cho user cụ thể không
        if (voucher.getAccount() != null && !voucher.getAccount().getUsername().equals(username)) {
            return ResponseEntity.badRequest().body("Mã giảm giá này không dành cho bạn!");
        }

        return ResponseEntity.ok(Map.of("discountPercent", voucher.getDiscountPercent()));
    }
}
