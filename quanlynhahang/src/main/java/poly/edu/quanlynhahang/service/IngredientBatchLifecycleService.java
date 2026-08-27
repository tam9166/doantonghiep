package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.IngredientBatchDisposalResponse;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.IngredientBatchDisposal;
import poly.edu.quanlynhahang.entity.IngredientBatchStatus;
import poly.edu.quanlynhahang.repository.IngredientBatchDisposalRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;

@Service
public class IngredientBatchLifecycleService {
    public record BulkDisposalResult(int processed, int failed, BigDecimal totalQuantity, String message) {}
    private final IngredientBatchRepository batchRepository;
    private final IngredientBatchDisposalRepository disposalRepository;
    private final IngredientRepository ingredientRepository;
    private final MenuAvailabilityService menuAvailabilityService;
    private final ActivityLogService activityLogService;

    public IngredientBatchLifecycleService(IngredientBatchRepository batchRepository,
                                           IngredientBatchDisposalRepository disposalRepository,
                                           IngredientRepository ingredientRepository,
                                           MenuAvailabilityService menuAvailabilityService,
                                           ActivityLogService activityLogService) {
        this.batchRepository = batchRepository;
        this.disposalRepository = disposalRepository;
        this.ingredientRepository = ingredientRepository;
        this.menuAvailabilityService = menuAvailabilityService;
        this.activityLogService = activityLogService;
    }

    @Scheduled(cron = "${restaurant.inventory.expiry-cron:0 5 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int synchronizeExpiredStatuses() {
        int changed = batchRepository.markExpired(new Date());
        // Keep the denormalized ingredient aggregate and menu availability in lockstep
        // with batch status changes made by the scheduler.
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            BigDecimal usable = batchRepository.sumAvailableByIngredientId(ingredient.getId());
            BigDecimal normalized = usable == null ? BigDecimal.ZERO : usable;
            if (ingredient.getQuantity() == null || ingredient.getQuantity().compareTo(normalized) != 0) {
                ingredient.setQuantity(normalized);
                ingredientRepository.save(ingredient);
            }
            menuAvailabilityService.refreshForIngredient(ingredient);
        }
        return changed;
    }

    @Transactional
    public IngredientBatchDisposalResponse dispose(Long batchId, String rawReason) {
        Date now = new Date();
        batchRepository.markExpired(now);
        IngredientBatch batch = batchRepository.findLockedById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lô nguyên liệu"));
        if (IngredientBatchStatus.DISPOSED.equals(batch.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lô nguyên liệu đã được tiêu hủy");
        }
        if (batch.getExpirationDate() == null || !batch.getExpirationDate().before(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ được tiêu hủy qua luồng này khi lô đã hết hạn");
        }
        BigDecimal quantity = batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity();
        if (quantity.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lô nguyên liệu không còn số lượng để tiêu hủy");
        }
        String reason = rawReason == null ? "" : rawReason.trim();
        if (reason.isBlank() || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do tiêu hủy phải từ 1 đến 500 ký tự");
        }

        Ingredient ingredient = batch.getIngredient();
        IngredientBatchDisposal disposal = new IngredientBatchDisposal();
        disposal.setBatch(batch);
        disposal.setIngredient(ingredient);
        disposal.setLotCode("LOT-" + batch.getId());
        disposal.setQuantityDisposed(quantity);
        disposal.setExpiryDate(batch.getExpirationDate());
        disposal.setDisposalDate(now);
        disposal.setReason(reason);
        disposal.setConfirmedBy(currentUsername());
        IngredientBatchDisposal saved = disposalRepository.save(disposal);

        batch.setQuantity(BigDecimal.ZERO);
        batch.setStatus(IngredientBatchStatus.DISPOSED);
        batchRepository.save(batch);
        BigDecimal usable = batchRepository.sumAvailableByIngredientId(ingredient.getId());
        ingredient.setQuantity(usable == null ? BigDecimal.ZERO : usable);
        ingredientRepository.save(ingredient);
        menuAvailabilityService.refreshForIngredient(ingredient);
        activityLogService.log("DISPOSE", "IngredientBatch", String.valueOf(batchId),
                "Tiêu hủy " + quantity + " " + ingredient.getUnit() + " - " + reason);
        return IngredientBatchDisposalResponse.from(saved);
    }

    @Transactional
    public BulkDisposalResult disposeAllExpired(String rawReason) {
        String reason = rawReason == null ? "" : rawReason.trim();
        if (reason.isBlank() || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do tiêu hủy phải từ 1 đến 500 ký tự");
        }
        Date now = new Date();
        batchRepository.markExpired(now);
        List<Long> ids = batchRepository.findPositiveBatchesWithIngredient().stream()
                .filter(b -> b.getExpirationDate() != null && b.getExpirationDate().before(now)
                        && !IngredientBatchStatus.DISPOSED.equals(b.getStatus()))
                .map(IngredientBatch::getId).toList();
        int processed = 0, failed = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : ids) {
            try {
                IngredientBatchDisposalResponse result = dispose(id, reason);
                processed++;
                total = total.add(result.quantityDisposed() == null ? BigDecimal.ZERO : result.quantityDisposed());
            } catch (ResponseStatusException ex) { failed++; }
        }
        return new BulkDisposalResult(processed, failed, total,
                "Đã xử lý " + processed + " lô hết hạn" + (failed > 0 ? "; " + failed + " lô đã được xử lý đồng thời hoặc không còn hợp lệ" : ""));
    }

    @Transactional(readOnly = true)
    public List<IngredientBatchDisposalResponse> history(Long batchId) {
        return disposalRepository.findByBatchIdOrderByDisposalDateDesc(batchId).stream()
                .map(IngredientBatchDisposalResponse::from).toList();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                ? authentication.getName() : "SYSTEM";
    }
}
