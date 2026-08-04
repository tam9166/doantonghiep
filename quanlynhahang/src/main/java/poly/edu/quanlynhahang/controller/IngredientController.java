package poly.edu.quanlynhahang.controller;

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

import java.util.Date;
import java.util.Calendar;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.dto.IngredientBatchCreateRequest;
import poly.edu.quanlynhahang.dto.IngredientBatchResponse;
import poly.edu.quanlynhahang.dto.IngredientResponse;
import poly.edu.quanlynhahang.dto.IngredientUpsertRequest;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.MenuAvailabilityService;
@RestController
@RequestMapping("/api/admin/ingredients")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private MenuAvailabilityService menuAvailabilityService;

    // 1. Lấy tất cả nguyên liệu
    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Ingredient> list = ingredientRepository.findAll();
        return ResponseEntity.ok(list.stream().map(IngredientResponse::from).toList());
    }

    // 2. Thêm nguyên liệu mới
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> create(@Valid @RequestBody IngredientUpsertRequest request) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.name().trim());
        ingredient.setUnit(request.unit().trim());
        ingredient.setMinStock(request.minStock());
        ingredient.setUnitPrice(request.unitPrice());
        ingredient.setImage(request.image());
        ingredient.setShelfLifeDays(request.shelfLifeDays());
        if (ingredient.getQuantity() == null) ingredient.setQuantity(0.0);
        if (ingredient.getMinStock() == null) ingredient.setMinStock(5.0);
        Ingredient saved = ingredientRepository.save(ingredient);
        activityLogService.log("CREATE", "Ingredient", String.valueOf(saved.getId()),
                "Thêm nguyên liệu mới: " + saved.getName() + " (" + saved.getUnit() + ")");
        return ResponseEntity.ok(IngredientResponse.from(saved));
    }

    // 3. Cập nhật nguyên liệu
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> addBatch(@PathVariable Long id, @Valid @RequestBody IngredientBatchCreateRequest request) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            IngredientBatch batch = new IngredientBatch();
            batch.setIngredient(ing);
            batch.setImportDate(new Date());
            batch.setQuantity(request.quantity());
            batch.setUnitPrice(request.unitPrice());
            batch.setExpirationDate(request.expirationDate());
            
            // Tự động tính ngày hết hạn nếu chưa có
            if (batch.getExpirationDate() == null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(batch.getImportDate());
                cal.add(Calendar.DAY_OF_YEAR, ing.getShelfLifeDays() != null ? ing.getShelfLifeDays() : 30);
                batch.setExpirationDate(cal.getTime());
            }
            
            IngredientBatch savedBatch = ingredientBatchRepository.save(batch);
            
            // Cập nhật lại tổng tồn kho
            double totalQuantity = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ing)
                    .stream().mapToDouble(IngredientBatch::getQuantity).sum();
            ing.setQuantity(totalQuantity);
            
            // Cập nhật giá nhập mới nhất vào bảng nguyên liệu chính để tham khảo
            if (batch.getUnitPrice() != null) {
                ing.setUnitPrice(batch.getUnitPrice());
            }
            ingredientRepository.save(ing);
            menuAvailabilityService.refreshForIngredient(ing);
            
            return ResponseEntity.ok(IngredientBatchResponse.from(savedBatch));
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
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
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysThreshold);
        Date targetDate = cal.getTime();
        
        List<IngredientBatch> expiring = ingredientBatchRepository.findExpiringBatches(targetDate);
        return ResponseEntity.ok(expiring.stream().map(IngredientBatchResponse::from).toList());
    }

    // 4.3. Xóa lô hàng
    @DeleteMapping("/batches/{batchId}")
    public ResponseEntity<?> deleteBatch(@PathVariable Long batchId) {
        Optional<IngredientBatch> batchOpt = ingredientBatchRepository.findById(batchId);
        if (batchOpt.isPresent()) {
            IngredientBatch batch = batchOpt.get();
            Ingredient ing = batch.getIngredient();
            ingredientBatchRepository.delete(batch);
            
            // Cập nhật lại số lượng tồn kho
            double totalQuantity = ingredientBatchRepository.findAvailableBatchesOrderByExpirationAsc(ing)
                    .stream().mapToDouble(IngredientBatch::getQuantity).sum();
            ing.setQuantity(totalQuantity);
            ingredientRepository.save(ing);
            menuAvailabilityService.refreshForIngredient(ing);
            
            return ResponseEntity.ok("Đã xóa lô hàng");
        }
        return ResponseEntity.badRequest().body("Không tìm thấy lô hàng!");
    }

    // 5. Cập nhật số lượng trực tiếp
    @PutMapping("/{id}/quantity")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> setQuantity(@PathVariable Long id, @RequestParam Double quantity) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            ing.setQuantity(quantity);
            return ResponseEntity.ok(IngredientResponse.from(ingredientRepository.save(ing)));
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
    }

    // 6. Xóa nguyên liệu
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
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
        List<Ingredient> all = ingredientRepository.findAll();
        long total = all.size();
        long lowStock = all.stream().filter(i -> {
            double q = i.getQuantity() != null ? i.getQuantity() : 0.0;
            double m = i.getMinStock() != null ? i.getMinStock() : 0.0;
            return q <= m;
        }).count();
        long outOfStock = all.stream().filter(i -> {
            double q = i.getQuantity() != null ? i.getQuantity() : 0.0;
            return q <= 0;
        }).count();

        // Đếm số lô sắp hết hạn trong 3 ngày
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 3);
        List<IngredientBatch> expiringBatches = ingredientBatchRepository.findExpiringBatches(cal.getTime());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("lowStock", lowStock);
        stats.put("outOfStock", outOfStock);
        stats.put("expiringBatchesCount", expiringBatches.size());
        return ResponseEntity.ok(stats);
    }
}
