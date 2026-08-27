package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Calendar;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.IngredientBatchStatus;
import poly.edu.quanlynhahang.dto.IngredientBatchCreateRequest;
import poly.edu.quanlynhahang.dto.IngredientBatchResponse;
import poly.edu.quanlynhahang.dto.IngredientBatchDisposalRequest;
import poly.edu.quanlynhahang.dto.IngredientBatchDisposalResponse;
import poly.edu.quanlynhahang.dto.IngredientResponse;
import poly.edu.quanlynhahang.dto.IngredientUpsertRequest;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchDisposalRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.InventoryAlertService;
import poly.edu.quanlynhahang.service.IngredientBatchLifecycleService;
import poly.edu.quanlynhahang.service.MenuAvailabilityService;
import poly.edu.quanlynhahang.service.InventoryImportService;
@RestController
@RequestMapping("/api/admin/ingredients")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    @Autowired
    private IngredientBatchDisposalRepository ingredientBatchDisposalRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private MenuAvailabilityService menuAvailabilityService;

    @Autowired
    private InventoryAlertService inventoryAlertService;
    @Autowired
    private IngredientBatchLifecycleService ingredientBatchLifecycleService;

    @Autowired
    private InventoryImportService inventoryImportService;

    // 1. Lấy tất cả nguyên liệu
    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Ingredient> list = ingredientRepository.findAll();
        return ResponseEntity.ok(list.stream().map(IngredientResponse::from).toList());
    }

    // 2. Thêm nguyên liệu mới
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> create(@Valid @RequestBody IngredientUpsertRequest request) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.name().trim());
        ingredient.setUnit(request.unit().trim());
        ingredient.setMinStock(request.minStock());
        ingredient.setUnitPrice(request.unitPrice());
        ingredient.setImage(request.image());
        ingredient.setShelfLifeDays(request.shelfLifeDays());
        if (ingredient.getQuantity() == null) ingredient.setQuantity(BigDecimal.ZERO);
        if (ingredient.getMinStock() == null) ingredient.setMinStock(new BigDecimal("5.0000"));
        Ingredient saved = ingredientRepository.save(ingredient);
        activityLogService.log("CREATE", "Ingredient", String.valueOf(saved.getId()),
                "Thêm nguyên liệu mới: " + saved.getName() + " (" + saved.getUnit() + ")");
        return ResponseEntity.ok(IngredientResponse.from(saved));
    }

    // 3. Cập nhật nguyên liệu
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody IngredientUpsertRequest details) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            String oldInfo = ing.getName() + " | minStock: " + ing.getMinStock() + " | unitPrice: " + ing.getUnitPrice();
            ing.setName(details.name().trim());
            ing.setUnit(details.unit().trim());
            ing.setMinStock(details.minStock());
            ing.setImage(details.image());
            if (details.unitPrice() != null) {
                ing.setUnitPrice(details.unitPrice());
            }
            if (details.shelfLifeDays() != null) {
                ing.setShelfLifeDays(details.shelfLifeDays());
            }
            Ingredient saved = ingredientRepository.save(ing);
            String newInfo = saved.getName() + " | minStock: " + saved.getMinStock() + " | unitPrice: " + saved.getUnitPrice();
            activityLogService.log("UPDATE", "Ingredient", String.valueOf(id),
                    "Cập nhật nguyên liệu: " + saved.getName(), oldInfo, newInfo);
            return ResponseEntity.ok(IngredientResponse.from(saved));
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
    }

    // 4. Nhập thêm hàng (Thêm Lô mới)
    @PostMapping("/{id}/batches")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> addBatch(@PathVariable Long id, @Valid @RequestBody IngredientBatchCreateRequest request) {
        if (!ingredientRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
        }
        IngredientBatch savedBatch = inventoryImportService.createSingleBatch(
                id, request.quantity(), request.unitPrice(), request.expirationDate(), "Nhập kho trực tiếp");
        return ResponseEntity.ok(IngredientBatchResponse.from(savedBatch));
    }

    // 4.1. Lấy danh sách lô hàng của 1 nguyên liệu
    @GetMapping("/{id}/batches")
    public ResponseEntity<?> getBatches(@PathVariable Long id) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            List<IngredientBatch> batches = ingredientBatchRepository.findByIngredientOrderByImportDateDesc(ingOpt.get());
            return ResponseEntity.ok(batches.stream().map(IngredientBatchResponse::from).toList());
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
    }

    // 4.2. Lấy danh sách lô hàng sắp hết hạn (trong vòng 3 ngày)
    @GetMapping("/expiring-batches")
    public ResponseEntity<?> getExpiringBatches(@RequestParam(defaultValue = "3") int daysThreshold) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysThreshold);
        Date targetDate = cal.getTime();
        
        List<IngredientBatch> expiring = ingredientBatchRepository.findExpiringBatchesBetween(now, targetDate);
        return ResponseEntity.ok(expiring.stream().map(IngredientBatchResponse::from).toList());
    }

    @GetMapping("/expired-batches")
    public ResponseEntity<List<IngredientBatchResponse>> getExpiredBatches() {
        Date now = new Date();
        List<IngredientBatch> expired = ingredientBatchRepository.findPositiveBatchesWithIngredient().stream()
                .filter(b -> b.getExpirationDate() != null && b.getExpirationDate().before(now)
                        && b.getStatus() != IngredientBatchStatus.DISPOSED)
                .toList();
        return ResponseEntity.ok(expired.stream().map(IngredientBatchResponse::from).toList());
    }

    @GetMapping("/disposed-batches")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<List<IngredientBatchDisposalResponse>> getDisposedBatches() {
        return ResponseEntity.ok(ingredientBatchDisposalRepository.findAllByOrderByDisposalDateDesc().stream()
                .map(IngredientBatchDisposalResponse::from).toList());
    }

    // 4.3. Xóa lô hàng
    @DeleteMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Transactional
    public ResponseEntity<?> deleteBatch(@PathVariable Long batchId) {
        Optional<IngredientBatch> batchOpt = ingredientBatchRepository.findLockedById(batchId);
        if (batchOpt.isPresent()) {
            IngredientBatch batch = batchOpt.get();
            if (batch.getStatus() == IngredientBatchStatus.EXPIRED
                    || batch.getStatus() == IngredientBatchStatus.DISPOSED) {
                return ResponseEntity.status(409).body("Lô hết hạn phải được xử lý qua quy trình tiêu hủy có lưu vết");
            }
            if (batch.getQuantity() != null && batch.getQuantity().signum() > 0) {
                return ResponseEntity.status(409).body("Chỉ được xóa lô trống; hãy điều chỉnh bằng nghiệp vụ kho phù hợp");
            }
            Ingredient ing = batch.getIngredient();
            ingredientBatchRepository.delete(batch);
            
            // Cập nhật lại số lượng tồn kho
            BigDecimal totalQuantity = sumBatchQuantity(ing);
            ing.setQuantity(totalQuantity);
            ingredientRepository.save(ing);
            menuAvailabilityService.refreshForIngredient(ing);
            
            return ResponseEntity.ok("Đã xóa lô hàng");
        }
        return ResponseEntity.badRequest().body("Không tìm thấy lô hàng!");
    }

    // 5. Cập nhật số lượng trực tiếp
    @PutMapping("/{id}/quantity")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> setQuantity(@PathVariable Long id, @RequestParam BigDecimal quantity) {
        if (!ingredientRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
        }
        return ResponseEntity.status(409).body(Map.of(
                "code", "BATCH_STOCK_IS_SOURCE_OF_TRUTH",
                "message", "Không thể sửa tổng tồn trực tiếp. Hãy nhập hoặc điều chỉnh lô nguyên liệu."));
    }

    // 6. Xóa nguyên liệu
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!ingredientRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
        }
        String name = ingredientRepository.findById(id).map(Ingredient::getName).orElse("#" + id);
        ingredientRepository.deleteById(id);
        activityLogService.log("DELETE", "Ingredient", String.valueOf(id),
                "Xóa nguyên liệu: " + name);
        return ResponseEntity.ok("Đã xóa nguyên liệu!");
    }

    // 7. Thống kê nhanh
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        InventoryAlertService.Analysis analysis = inventoryAlertService.analyze(3);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", analysis.total());
        stats.put("lowStock", analysis.lowStock());
        stats.put("outOfStock", analysis.outOfStock());
        stats.put("expiringBatchesCount", analysis.expiringBatchesCount());
        stats.put("expiredBatchesCount", analysis.expiredBatchesCount());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/batches/{batchId}/dispose")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<IngredientBatchDisposalResponse> disposeExpiredBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody IngredientBatchDisposalRequest request) {
        return ResponseEntity.ok(ingredientBatchLifecycleService.dispose(batchId, request.reason()));
    }

    @PostMapping("/expired-batches/dispose-all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<IngredientBatchLifecycleService.BulkDisposalResult> disposeAllExpired(
            @Valid @RequestBody IngredientBatchDisposalRequest request) {
        return ResponseEntity.ok(ingredientBatchLifecycleService.disposeAllExpired(request.reason()));
    }

    @GetMapping("/batches/{batchId}/disposals")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<List<IngredientBatchDisposalResponse>> getDisposalHistory(@PathVariable Long batchId) {
        return ResponseEntity.ok(ingredientBatchLifecycleService.history(batchId));
    }

    @GetMapping("/analysis")
    public ResponseEntity<InventoryAlertService.Analysis> getInventoryAnalysis(
            @RequestParam(defaultValue = "3") int expiringDays) {
        int safeThreshold = Math.max(1, Math.min(expiringDays, 30));
        return ResponseEntity.ok(inventoryAlertService.analyze(safeThreshold));
    }

    private BigDecimal sumBatchQuantity(Ingredient ingredient) {
        return ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ingredient).stream()
                .map(IngredientBatch::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
