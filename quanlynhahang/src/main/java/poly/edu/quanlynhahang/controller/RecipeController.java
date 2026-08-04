package poly.edu.quanlynhahang.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.dto.RecipeResponse;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.service.MenuAvailabilityService;
@RestController
@RequestMapping("/api/admin/recipes")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
public class RecipeController {

    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private MenuAvailabilityService menuAvailabilityService;

    // 1. Lấy tất cả công thức
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(recipeRepository.findAll().stream().map(RecipeResponse::from).toList());
    }

    // 2. Lấy công thức theo productId
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProduct(@PathVariable Integer productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy món ăn!");
        }
        List<Recipe> recipes = recipeRepository.findByProduct(productOpt.get());
        return ResponseEntity.ok(recipes.stream().map(RecipeResponse::from).toList());
    }

    // 3. Thêm công thức mới
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> create(@RequestBody RecipeRequest request) {
        Optional<Product> productOpt = productRepository.findById(request.getProductId());
        Optional<Ingredient> ingredientOpt = ingredientRepository.findById(request.getIngredientId());

        if (productOpt.isEmpty()) return ResponseEntity.badRequest().body("Không tìm thấy món ăn!");
        if (ingredientOpt.isEmpty()) return ResponseEntity.badRequest().body("Không tìm thấy nguyên liệu!");

        Recipe recipe = new Recipe();
        recipe.setProduct(productOpt.get());
        recipe.setIngredient(ingredientOpt.get());
        recipe.setAmountRequired(request.getAmountRequired());

        Recipe saved = recipeRepository.save(recipe);
        menuAvailabilityService.refreshProduct(saved.getProduct());
        return ResponseEntity.ok(RecipeResponse.from(saved));
    }

    // 4. Cập nhật lượng nguyên liệu trong công thức
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RecipeRequest request) {
        var recipeOpt = recipeRepository.findById(id.intValue());
        if (recipeOpt.isPresent()) {
            Recipe recipe = recipeOpt.get();
            recipe.setAmountRequired(request.getAmountRequired());
            Recipe saved = recipeRepository.save(recipe);
            menuAvailabilityService.refreshProduct(saved.getProduct());
            return ResponseEntity.ok(RecipeResponse.from(saved));
        }
        return ResponseEntity.badRequest().body("Không tìm thấy công thức!");
    }

    // 5. Xóa công thức
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!recipeRepository.existsById(id.intValue())) {
            return ResponseEntity.badRequest().body("Không tìm thấy công thức!");
        }
        Recipe recipe = recipeRepository.findById(id.intValue()).orElseThrow();
        Product product = recipe.getProduct();
        recipeRepository.delete(recipe);
        recipeRepository.flush();
        menuAvailabilityService.refreshProduct(product);
        return ResponseEntity.ok("Đã xóa công thức!");
    }

    // DTO cho request
    public static class RecipeRequest {
        private Integer productId;
        private Long ingredientId;
        private Double amountRequired;

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }
        public Long getIngredientId() { return ingredientId; }
        public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
        public Double getAmountRequired() { return amountRequired; }
        public void setAmountRequired(Double amountRequired) { this.amountRequired = amountRequired; }
    }
}
