package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.ProductUpsertRequest;
import poly.edu.quanlynhahang.dto.AdminProductResponse;
import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.MenuEconomicsService;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ActivityLogService activityLogService;
    private final MenuEconomicsService menuEconomicsService;

    public AdminProductController(ProductRepository productRepository,
                                  CategoryRepository categoryRepository,
                                  ReviewRepository reviewRepository,
                                  ActivityLogService activityLogService,
                                  MenuEconomicsService menuEconomicsService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.activityLogService = activityLogService;
        this.menuEconomicsService = menuEconomicsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public List<AdminProductResponse> getProductsForOperations() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            Double average = reviewRepository.getAverageRatingByProductId(product.getId());
            product.setAverageRating(average == null ? 0.0 : Math.round(average * 10.0) / 10.0);

        }
        return products.stream().map(product -> {
            MenuEconomicsService.Assessment assessment = menuEconomicsService.assess(product);
            return AdminProductResponse.from(product, assessment.costPrice(),
                    assessment.availableServings(), assessment.hasRecipe());
        }).toList();
    }

    @PostMapping
    public ResponseEntity<AdminProductResponse> addProduct(@Valid @RequestBody ProductUpsertRequest request) {
        Product product = new Product();
        applyRequest(product, request, true);
        product.setStatus(false);
        product.setAvailable(false);
        Product saved = productRepository.save(product);
        activityLogService.log("CREATE", "Product", String.valueOf(saved.getId()),
                "Them mon an moi: " + saved.getName() + " - Gia: " + saved.getPrice());
        return ResponseEntity.ok(AdminProductResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND"));
        productRepository.delete(product);
        activityLogService.log("DELETE", "Product", String.valueOf(id), "Xoa mon an: " + product.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProductResponse> updateProduct(@PathVariable Integer id,
                                                              @Valid @RequestBody ProductUpsertRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND"));
        String oldInfo = product.getName() + " | Gia: " + product.getPrice();
        applyRequest(product, request, false);
        if (Boolean.TRUE.equals(request.status()) || Boolean.TRUE.equals(request.available())) {
            menuEconomicsService.requireSellable(product);
        }
        Product saved = productRepository.save(product);
        activityLogService.log("UPDATE", "Product", String.valueOf(id),
                "Cap nhat mon an: " + saved.getName(), oldInfo,
                saved.getName() + " | Gia: " + saved.getPrice());
        return ResponseEntity.ok(AdminProductResponse.from(saved));
    }

    @PutMapping("/{id}/toggle-available")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<String> toggleAvailable(@PathVariable Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND"));
        boolean available = !Boolean.TRUE.equals(product.getAvailable());
        if (available) {
            menuEconomicsService.requireSellable(product);
        }
        product.setAvailable(available);
        product.setStatus(available);
        productRepository.save(product);
        return ResponseEntity.ok(available ? "AVAILABLE" : "UNAVAILABLE");
    }

    @PostMapping("/signature-dishes/refresh")
    public ResponseEntity<List<AdminProductResponse>> refreshSignatureDishes(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Integer> productIds = productRepository.findTopSellingProductIds(safeLimit);
        List<Product> products = productRepository.findAll();
        products.forEach(product -> product.setIsSignatureDish(productIds.contains(product.getId())));
        productRepository.saveAll(products);
        activityLogService.log("REFRESH", "Product", "signature-dishes",
                "Cap nhat mon dac trung tu " + productIds.size() + " mon ban chay");
        return ResponseEntity.ok(products.stream().map(AdminProductResponse::from).toList());
    }

    private void applyRequest(Product product, ProductUpsertRequest request, boolean creating) {
        Category category = categoryRepository.findById(request.category().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND"));
        product.setName(request.name().trim());
        product.setPrice(request.price());
        product.setTaxRate(request.taxRate() == null ? new BigDecimal("8.00") : request.taxRate());
        product.setDescription(request.description() == null ? null : request.description().trim());
        product.setImage(request.image() == null ? null : request.image().trim());
        product.setCategory(category);
        if (request.status() != null || creating) {
            product.setStatus(request.status() == null || request.status());
        }
        if (request.available() != null || creating) {
            product.setAvailable(request.available() == null || request.available());
        }
        product.setDietType(request.dietType() == null ? poly.edu.quanlynhahang.entity.DietType.MAN : request.dietType());
        product.setCookingMethod(request.cookingMethod() == null ? poly.edu.quanlynhahang.entity.CookingMethod.KHAC : request.cookingMethod());
        product.setSpicyLevel(request.spicyLevel() == null ? 0 : request.spicyLevel());
        product.setIsSignatureDish(Boolean.TRUE.equals(request.isSignatureDish()));
    }
}
