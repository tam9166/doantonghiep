package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.*;
import poly.edu.quanlynhahang.repository.*;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.InventoryAlertService;
import poly.edu.quanlynhahang.service.NotificationService;

import java.util.*;
@RestController
@RequestMapping("/api/admin/purchase-suggestions")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
public class PurchaseSuggestionController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    @Autowired
    private InventoryAlertService inventoryAlertService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Lấy danh sách đề xuất mua hàng tự động
     * Logic: Nguyên liệu dưới minStock + tính tốc độ tiêu thụ 7 ngày qua
     */
    @GetMapping
    public ResponseEntity<?> getSuggestions() {
        return ResponseEntity.ok(inventoryAlertService.analyze(3));
    }

    /**
     * Duyệt đề xuất: Tự động tạo lô nhập kho cho nguyên liệu
     */
    @PostMapping("/approve/{ingredientId}")
    public ResponseEntity<?> approveSuggestion(
            @PathVariable Long ingredientId,
            @RequestParam BigDecimal quantity) {

        Optional<Ingredient> ingOpt = ingredientRepository.findById(ingredientId);
        if (ingOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
        }

        Ingredient ing = ingOpt.get();

        // Tạo lô nhập kho mới
        IngredientBatch batch = new IngredientBatch();
        batch.setIngredient(ing);
        batch.setQuantity(quantity);
        batch.setUnitPrice(ing.getUnitPrice());
        batch.setImportDate(new Date());

        // Tính hạn sử dụng
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, ing.getShelfLifeDays() != null ? ing.getShelfLifeDays() : 30);
        batch.setExpirationDate(cal.getTime());

        ingredientBatchRepository.save(batch);

        // Cập nhật tổng tồn kho
        BigDecimal totalQuantity = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ing)
                .stream().map(IngredientBatch::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ing.setQuantity(totalQuantity);
        ingredientRepository.save(ing);

        // Ghi log
        activityLogService.log("CREATE", "PurchaseSuggestion", String.valueOf(ingredientId),
                "Duyệt đề xuất mua hàng: " + ing.getName() + " - SL: " + quantity + " " + ing.getUnit());

        return ResponseEntity.ok(Map.of(
                "message", "Đã duyệt và nhập kho " + quantity + " " + ing.getUnit() + " " + ing.getName(),
                "newStock", totalQuantity
        ));
    }
}
