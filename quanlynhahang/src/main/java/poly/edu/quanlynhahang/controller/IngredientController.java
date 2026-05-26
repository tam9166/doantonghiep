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

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.repository.IngredientRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/ingredients")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;

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

    // 4. Nhập thêm hàng (cộng số lượng)
    @PutMapping("/{id}/restock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> restock(@PathVariable Long id, @RequestParam Double amount) {
        var ingOpt = ingredientRepository.findById(id);
        if (ingOpt.isPresent()) {
            Ingredient ing = ingOpt.get();
            ing.setQuantity(ing.getQuantity() + amount);
            ingredientRepository.save(ing);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Đã nhập thêm " + amount + " " + ing.getUnit() + " " + ing.getName());
            result.put("newQuantity", ing.getQuantity());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");
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

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("lowStock", lowStock);
        stats.put("outOfStock", outOfStock);
        return ResponseEntity.ok(stats);
    }
}
