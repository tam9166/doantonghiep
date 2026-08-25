package poly.edu.quanlynhahang.service;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.VoucherUpsertRequest;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.VoucherRepository;

@Service
public class VoucherLifecycleService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final VoucherRepository repository;
    private final Clock clock;

    @Autowired
    public VoucherLifecycleService(VoucherRepository repository) {
        this(repository, Clock.system(BUSINESS_ZONE));
    }

    VoucherLifecycleService(VoucherRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void configure(Voucher voucher, VoucherUpsertRequest request) {
        if (request.startAt() != null && request.endAt() != null
                && !request.startAt().before(request.endAt())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        int used = safeUsed(voucher);
        if (request.usageLimit() != null && request.usageLimit() < used) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Giới hạn mới không thể nhỏ hơn số lượt đã dùng");
        }
        voucher.setUsageLimit(request.usageLimit());
        voucher.setStartAt(request.startAt());
        voucher.setEndAt(request.endAt());
        voucher.setActive(request.active() == null ? Boolean.TRUE : request.active());
        voucher.setUsedCount(used);
        voucher.setIsUsed(isExhausted(voucher));
        if (Boolean.TRUE.equals(voucher.getActive())) requireCanEnable(voucher);
    }

    public void validateForUse(Voucher voucher, String username) {
        String status = statusOf(voucher, now());
        if (!"ACTIVE".equals(status)) {
            String message = switch (status) {
                case "PAUSED" -> "Voucher đang tạm dừng";
                case "NOT_STARTED" -> "Voucher chưa có hiệu lực";
                case "EXHAUSTED" -> "Voucher đã hết lượt sử dụng";
                case "EXPIRED" -> "Voucher đã hết hạn";
                default -> "Voucher không khả dụng";
            };
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
        if (voucher.getAccount() != null
                && (username == null || !voucher.getAccount().getUsername().equals(username))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voucher không thuộc tài khoản này");
        }
        if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent() <= 0
                || voucher.getDiscountPercent() > 100) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher có mức giảm không hợp lệ");
        }
    }

    public void redeemLocked(Voucher voucher, String username) {
        validateForUse(voucher, username);
        int next = safeUsed(voucher) + 1;
        if (voucher.getUsageLimit() != null && next > voucher.getUsageLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher đã hết lượt sử dụng");
        }
        voucher.setUsedCount(next);
        voucher.setIsUsed(isExhausted(voucher));
        repository.save(voucher);
    }

    @Transactional
    public Voucher redeem(String code, String username) {
        Voucher voucher = repository.findLockedByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        redeemLocked(voucher, username);
        return voucher;
    }

    @Transactional
    public Voucher toggle(Long id, boolean active) {
        Voucher voucher = locked(id);
        if (active) requireCanEnable(voucher);
        voucher.setActive(active);
        return repository.save(voucher);
    }

    @Transactional
    public Voucher resetUsage(Long id) {
        Voucher voucher = locked(id);
        voucher.setUsedCount(0);
        voucher.setIsUsed(false);
        return repository.save(voucher);
    }

    public void requireCanEnable(Voucher voucher) {
        Date now = now();
        if (voucher.getEndAt() != null && now.after(voucher.getEndAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể bật voucher đã hết hạn");
        }
        if (isExhausted(voucher)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hãy tăng giới hạn hoặc đặt lại lượt dùng trước khi bật voucher");
        }
    }

    public static String statusOf(Voucher voucher, Date now) {
        if (!Boolean.TRUE.equals(voucher.getActive())) return "PAUSED";
        if (voucher.getStartAt() != null && now.before(voucher.getStartAt())) return "NOT_STARTED";
        if (voucher.getEndAt() != null && now.after(voucher.getEndAt())) return "EXPIRED";
        if (isExhausted(voucher)) return "EXHAUSTED";
        return "ACTIVE";
    }

    private static boolean isExhausted(Voucher voucher) {
        if (voucher.getUsageLimit() != null) return safeUsed(voucher) >= voucher.getUsageLimit();
        return Boolean.TRUE.equals(voucher.getIsUsed());
    }

    private static int safeUsed(Voucher voucher) {
        return voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
    }

    private Voucher locked(Long id) {
        return repository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
    }

    private Date now() {
        return Date.from(clock.instant());
    }
}
