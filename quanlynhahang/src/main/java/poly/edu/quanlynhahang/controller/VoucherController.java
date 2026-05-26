package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Admin: Get all vouchers
    @GetMapping("/admin")
    public ResponseEntity<?> getAllVouchers() {
        return ResponseEntity.ok(voucherRepository.findAll());
    }

    // User: Get my active vouchers
    @GetMapping("/my-vouchers")
    public ResponseEntity<?> getMyVouchers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Voucher> myVouchers = voucherRepository.findByAccountUsername(username);
        return ResponseEntity.ok(myVouchers);
    }

    // Auto-generate voucher when winning Lucky Wheel
    @PostMapping("/generate")
    public ResponseEntity<?> generateVoucher(@RequestBody Map<String, Integer> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Integer discountPercent = payload.get("discount");

        if (discountPercent == null || discountPercent <= 0) {
            return ResponseEntity.badRequest().body("Phần trăm giảm giá không hợp lệ");
        }

        Optional<Account> accOpt = accountRepository.findById(username);
        if (!accOpt.isPresent()) return ResponseEntity.badRequest().body("Tài khoản không tồn tại");

        String code = "LUCKY-" + discountPercent + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setDiscountPercent(discountPercent);
        voucher.setCreateDate(new Date());
        voucher.setIsUsed(false);
        voucher.setAccount(accOpt.get());

        voucherRepository.save(voucher);
        return ResponseEntity.ok(Map.of("message", "Tạo voucher thành công", "code", code));
    }

    // Admin: Manually create voucher
    @PostMapping("/admin/create")
    public ResponseEntity<?> adminCreateVoucher(@RequestBody Voucher voucherRequest) {
        if (voucherRequest.getCode() == null || voucherRequest.getCode().isEmpty()) {
            voucherRequest.setCode("CODE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        voucherRequest.setCreateDate(new Date());
        voucherRequest.setIsUsed(false);
        
        // Nếu admin chỉ định user cụ thể
        if (voucherRequest.getAccount() != null && voucherRequest.getAccount().getUsername() != null) {
            Optional<Account> accOpt = accountRepository.findById(voucherRequest.getAccount().getUsername());
            if (accOpt.isPresent()) {
                voucherRequest.setAccount(accOpt.get());
            } else {
                voucherRequest.setAccount(null);
            }
        }

        Voucher saved = voucherRepository.save(voucherRequest);
        return ResponseEntity.ok(saved);
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
