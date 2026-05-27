package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

import java.util.Date;
import java.util.Calendar;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/ingredients")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    // 1. Lấy tất cả nguyên liệu
    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Ingredient> list = ingredientRepository.findAll();
        return ResponseEntity.ok(list);
    }

    // 2. Thêm nguyên liệu mới
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> create(@RequestBody Ingredient ingredient) {
        if (ingredient.getQuantity() == null) ingredient.setQuantity(0.0);
        if (ingredient.getMinStock() == null) ingredient.setMinStock(5.0);
        return ResponseEntity.ok(ingredientRepository.save(ingredient));
    }

    // 3. Cập nhật nguyên liệu
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Ingredient details) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            ing.setName(details.getName());
            ing.setUnit(details.getUnit());
            ing.setMinStock(details.getMinStock());
            ing.setImage(details.getImage());
            if (details.getUnitPrice() != null) {
                ing.setUnitPrice(details.getUnitPrice());
            }
            return ResponseEntity.ok(ingredientRepository.save(ing));
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
    }

    // 4. Nhập thêm hàng (Thêm Lô mới)
    @PostMapping("/{id}/batches")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> addBatch(@PathVariable Long id, @RequestBody IngredientBatch batch) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            batch.setIngredient(ing);
            batch.setImportDate(new Date());
            
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
            
            return ResponseEntity.ok(savedBatch);
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
    }

    // 4.1. Lấy danh sách lô hàng của 1 nguyên liệu
    @GetMapping("/{id}/batches")
    public ResponseEntity<?> getBatches(@PathVariable Long id) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            List<IngredientBatch> batches = ingredientBatchRepository.findByIngredientOrderByImportDateDesc(ingOpt.get());
            return ResponseEntity.ok(batches);
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
        return ResponseEntity.ok(expiring);
    }

    // 5. Cập nhật số lượng trực tiếp
    @PutMapping("/{id}/quantity")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> setQuantity(@PathVariable Long id, @RequestParam Double quantity) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            ing.setQuantity(quantity);
            return ResponseEntity.ok(ingredientRepository.save(ing));
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
        ingredientRepository.deleteById(id);
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
