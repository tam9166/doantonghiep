package poly.edu.quanlynhahang.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class AdminProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public List<Product> getProductsForOperations() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            Double average = reviewRepository.getAverageRatingByProductId(product.getId());
            product.setAverageRating(average == null ? 0.0 : Math.round(average * 10.0) / 10.0);

            double recipeCost = recipeRepository.findByProduct(product).stream()
                    .filter(recipe -> recipe.getIngredient() != null
                            && recipe.getIngredient().getUnitPrice() != null
                            && recipe.getAmountRequired() != null)
                    .mapToDouble(recipe -> recipe.getIngredient().getUnitPrice() * recipe.getAmountRequired())
                    .sum();
            if (recipeCost > 0) {
                product.setCostPrice(recipeCost);
            }
        }
        return products;
    }

    // API Thêm món ăn mới
    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        activityLogService.log("CREATE", "Product", String.valueOf(savedProduct.getId()),
                "Thêm món ăn mới: " + savedProduct.getName() + " - Giá: " + savedProduct.getPrice() + "đ");
        return ResponseEntity.ok(savedProduct);
    }

    // API Xóa món ăn theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Không tìm thấy món ăn này!");
        }
        String productName = productRepository.findById(id).map(Product::getName).orElse("#" + id);
        productRepository.deleteById(id);
        activityLogService.log("DELETE", "Product", String.valueOf(id),
                "Xóa món ăn: " + productName);
        return ResponseEntity.ok("Đã xóa món ăn thành công!");
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));
        
        String oldInfo = product.getName() + " | Giá: " + product.getPrice();
        product.setName(productDetails.getName());
        product.setPrice(productDetails.getPrice());
        product.setDescription(productDetails.getDescription());
        product.setImage(productDetails.getImage());
        product.setStatus(productDetails.getStatus());
        
        // Nếu có category thì cập nhật
        if (productDetails.getCategory() != null) {
            product.setCategory(productDetails.getCategory());
        }
        
        Product saved = productRepository.save(product);
        String newInfo = saved.getName() + " | Giá: " + saved.getPrice();
        activityLogService.log("UPDATE", "Product", String.valueOf(id),
                "Cập nhật món ăn: " + saved.getName(), oldInfo, newInfo);
        return ResponseEntity.ok(saved);
    }

    // 🌟 API MỚI: Bếp báo hết/còn món (Kitchen toggle available)
    @PutMapping("/{id}/toggle-available")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN')")
    public ResponseEntity<?> toggleAvailable(@PathVariable Integer id) {
        return productRepository.findById(id).map(product -> {
            product.setAvailable(!product.getAvailable());
            product.setStatus(product.getAvailable()); // Sync status với available
            productRepository.save(product);
            String msg = product.getAvailable() ? "✅ Đã mở bán lại: " : "❌ Đã báo hết: ";
            return ResponseEntity.ok(msg + product.getName());
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy món ăn!"));
    }
}
