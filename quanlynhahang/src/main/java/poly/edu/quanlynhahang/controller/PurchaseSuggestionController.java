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
import poly.edu.quanlynhahang.service.InventoryImportService;

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

    @Autowired
    private InventoryImportService inventoryImportService;

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

        if (quantity == null || quantity.signum() <= 0) {
            return ResponseEntity.badRequest().body("Số lượng nhập phải lớn hơn 0");
        }
        Optional<Ingredient> ingOpt = ingredientRepository.findById(ingredientId);
        if (ingOpt.isEmpty()) return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
        Ingredient ing = ingOpt.get();
        IngredientBatch batch = inventoryImportService.createSingleBatch(
                ingredientId, quantity, ing.getUnitPrice(), null, "Duyệt đề xuất mua hàng");
        BigDecimal totalQuantity = ingredientBatchRepository.sumAvailableByIngredientId(ingredientId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã duyệt và nhập kho " + quantity + " " + ing.getUnit() + " " + ing.getName(),
                "newStock", totalQuantity
        ));
    }
}
